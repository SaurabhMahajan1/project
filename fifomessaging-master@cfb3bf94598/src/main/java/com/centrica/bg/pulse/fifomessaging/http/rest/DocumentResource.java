package com.centrica.bg.pulse.fifomessaging.http.rest;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import com.centrica.bg.pulse.fifomessaging.entity.GroupedMessageContainer;
import com.centrica.bg.pulse.fifomessaging.entity.MessageWrapper;
import com.google.gson.*;
import io.swagger.annotations.*;
import org.apache.activemq.ScheduledMessage;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Path("/")
@Api(tags = "Message Endpoint")
public class DocumentResource extends AbstractResource {

    public static final String UNDERSCORE = "_";
    public static final String GROUP = "group";
    public static final String DATA = "data";

    private static long EXPIRY_TIME = 30000;


    private static Queue<String> wmisQ = new ConcurrentLinkedQueue<>();
    private static Queue<String> taskForceQ = new ConcurrentLinkedQueue<>();
    private static Queue<String> sapPIQ = new ConcurrentLinkedQueue<>();
    private static Queue<String> tempCouchbaseQ = new ConcurrentLinkedQueue<>();


    private static Map<String, Queue<String>> collectionAndQueueMapper = new HashMap<>();

    static {
        collectionAndQueueMapper.put(Constants.WMISQ_COLLECTION_MAPPER, wmisQ);
        collectionAndQueueMapper.put(Constants.TF_COLLECTION_MAPPER, taskForceQ);
        collectionAndQueueMapper.put(Constants.PI_COLLECTION_MAPPER, sapPIQ);
        collectionAndQueueMapper.put(Constants.CB_COLLECTION_MAPPER, tempCouchbaseQ);
    }

    @Autowired
    ConnectionFactory connectionFactory;

    private Map<String, MessageWrapper> msgStoreForGroups = new HashMap<>();
    private Map<String, GroupedMessageContainer> groupContainerStore = new HashMap<>();
    private Set<String> queueContentChecker = new HashSet<>();
    private Set<String> msgInProcessByClientSet = new HashSet<>();
    @Context
    private UriInfo uriInfo;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired()
    @Qualifier("msgInProgressQueue")
    private javax.jms.Queue msgInProgressQueue;

    public DocumentResource() {
        this.log = LogFactory.getLog(DocumentResource.class);
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }


    @Path("/message/fifo/group/namespace/fft/collection/{collectionName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "send message in group", notes = "Accepts message with a group id, " +
            "all other messages with same group id will delivered in ordered sequence " +
            "and only to one consumer at a time"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400,
                    message = "Bad Request, if invalid collection name or invalid payload"),
            @ApiResponse(code = 422,
                    message = "The request was well-formed but, group or data attribute missing"),
            @ApiResponse(code = 201,
                    message = "Created"
            ),
    })
    public Response sendMessage(@PathParam("collectionName") String collectionName, @RequestBody Map<String, Object> body) {
        Response.ResponseBuilder builder;
        System.out.println(body);
        Map<String, Object> messageDocument = MapUtils.isNotEmpty(body)?(Map<String, Object>) body.get("message"):null ;
        log.debug("body size "+(MapUtils.isNotEmpty(body)?body.size():0)+"message is "+messageDocument);

        log.debug(" msgDoc size is "+(MapUtils.isNotEmpty(messageDocument)?messageDocument.size():0));
        // collectionName is mapped to Queue Name
        //Interface contract is to have group as String. This can also be checked in JSONSchema and returned
        // as invalid request. If group is not string.
        if(MapUtils.isNotEmpty(body) && body.size()==1 && MapUtils.isNotEmpty(messageDocument) && messageDocument.size()<=2 ) {

            JsonParser jsonParser = new JsonParser();
            JsonElement data;
            Gson gson = new GsonBuilder().create();
            try {

                data = jsonParser.parse(gson.toJson(messageDocument.get(DATA)));
                log.debug("data after parse is " + data);

            } catch (Exception e) {
                log.error("Error while parsing data " + e.getMessage());
                Response.ResponseBuilder responseBuilder = dataParsingException(e);
                return responseBuilder.build();
            }
            // Map<String,Object> localMsgDocument;
            MessageWrapper messageWrapper;
            Map<String, Object> metaDetails;
            String localMsgDocumentId;
            String groupMsgContainerId;
            String groupIdentifier = messageDocument.get(GROUP) instanceof String ? messageDocument.get(GROUP).toString() : null;
            if (groupIdentifier != null && groupIdentifier.length() > 0 && !data.isJsonNull()) {
                if ((collectionName != null && collectionName.length() > 0 && collectionAndQueueMapper.containsKey(collectionName))) {

                    DateTime currentTime = DateTime.now();
                    localMsgDocumentId = generateMsgDocumentId(collectionName, groupIdentifier, currentTime);
                    groupMsgContainerId = generateGroupMsgContainerId(collectionName, groupIdentifier);

                    metaDetails = getMesssageMetaDetails(localMsgDocumentId, currentTime);

                    //Create a local document and place message and meta inside it
                    messageWrapper = generateMessageWrapper(messageDocument, metaDetails, groupMsgContainerId);

                    persistMessage(localMsgDocumentId, messageWrapper);

                    //create container group document with attributes id and array of ordered messages for this group.
                    addMessageToGroupContainer(messageWrapper, groupMsgContainerId);

                    if (!isGroupDocumentQueued(groupMsgContainerId)) {
                        //add to queue
                        collectionAndQueueMapper.get(collectionName).add(groupMsgContainerId);
                        queueContentChecker.add(groupMsgContainerId);
                    }

                    log.debug("current status of wmisQ : " + wmisQ);
                    log.debug("current status of TaskforceQ : " + taskForceQ);
                    builder = Response.status(Response.Status.CREATED);
                    builder.entity(getMessageResponse(messageWrapper));
                    addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
                    return builder.build();
                } else {
                    //invalid message
                    builder = noSuchCollectionResponse();
                    return builder.build();
                }
            } else {
                log.debug("group is" + groupIdentifier + " data is " + data);
                builder = Response.status(422);
                builder.entity(produceErrorMessage(422, 422, "The request was well-formed, but group or data attribute missing"));
                return builder.build();
            }
        }else{
            Response.ResponseBuilder invalidRequestPayload = invalidPayload();
            return invalidRequestPayload.build();
        }


    }

    private Response.ResponseBuilder invalidPayload() {
        Response.ResponseBuilder invalidRequestPayload= Response.status(Response.Status.BAD_REQUEST);
        JsonParser jsonParser = new JsonParser();
        JsonElement requestStructure = jsonParser.parse("{message:{group:groudId,data:{}}}");

        invalidRequestPayload.entity(produceErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getStatusCode(),"Invalid payload, should be in format " +requestStructure));
        addCharacterEncoding(invalidRequestPayload, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return invalidRequestPayload;
    }

    private void persistMessage(String localMsgDocumentId, MessageWrapper messageWrapper) {
        msgStoreForGroups.put(localMsgDocumentId, messageWrapper);
    }

    private void addMessageToGroupContainer(MessageWrapper messageWrapper, String groupMsgContainerId) {
        // Map<String,Object> groupedMsgContainerDocument = groupContainerStore.get(groupMsgContainerId);
        GroupedMessageContainer groupedMsgContainerDocument = groupContainerStore.get(groupMsgContainerId);
        if (groupedMsgContainerDocument == null) {
            log.debug("GroupedMessageContainer is null creating new");
            groupedMsgContainerDocument = new GroupedMessageContainer();
            groupedMsgContainerDocument.setDocId(groupMsgContainerId);
        }

        groupedMsgContainerDocument.addMessage(messageWrapper.getMessageId(), (HashMap) messageWrapper.getMeta());
        // groupedMsgContainerDocument.setMesssageStore_deleteit(msgStoreForGroups);

        groupContainerStore.put(groupMsgContainerId, groupedMsgContainerDocument);
    }

    private String generateMsgDocumentId(@QueryParam("collection") String collectionName, String groupIdentifier, DateTime currentTime) {
        return collectionName + UNDERSCORE + groupIdentifier + UNDERSCORE + Util.getDateInString(currentTime, Constants.FILE_NAME_DATE_PATTERN);
    }

    private MessageWrapper generateMessageWrapper(@RequestBody Map<String, Object> messageDocument, Map<String, Object> metaDetails, String groupMsgContainerId) {
        MessageWrapper messageWrapper;
        messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(messageDocument);
        messageWrapper.setMeta(metaDetails);
        messageWrapper.setContainerReference(groupMsgContainerId);
        return messageWrapper;
    }

    private Map<String, Object> getMesssageMetaDetails(String localMsgDocumentId, DateTime currentTime) {
        Map<String, Object> metaDetails;
        metaDetails = new HashMap<>();
        metaDetails.put(Constants.MSG_ID, localMsgDocumentId);
        metaDetails.put(Constants.CREATE_TIMESTAMP, Util.generateISO8601(currentTime));
        return metaDetails;
    }

    private String generateGroupMsgContainerId(@QueryParam("collection") String collectionName, String groupIdentifier) {
        return collectionName + UNDERSCORE + groupIdentifier;
    }


    @Path("/message/fifo/group/namespace/fft/collection/{collectionName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get message in order from group", notes = "returns one message at a time, in order it was received "
    )
    @ApiResponses(value = {
            @ApiResponse(code = 302,
                    message = "Found"),
            @ApiResponse(code = 204,
                    message = "Not Found"
            ),
            @ApiResponse(code = 400,
                    message = "Bad Request")})
    public Response fetchMessage(@PathParam("collectionName") String collectionName) {
        Response.ResponseBuilder builder = Response.status(Response.Status.FOUND);

        //fetch queue reference
        Queue<String> targetQueue = collectionAndQueueMapper.get(collectionName);
        String groupMsgContainerId = null;
        MessageWrapper messageWrapper = null;
        Map<String, Object> responseObject = new HashMap<>();
        if ((collectionName != null && collectionName.length() > 0 && collectionAndQueueMapper.containsKey(collectionName))) {
            try {
                //each call should immediately remove item from queue and in case of exception put it back.
                groupMsgContainerId = targetQueue.poll();
                log.debug("in fetchMessage fetching " + groupMsgContainerId + " from queue containing elements " + targetQueue);
                if (groupMsgContainerId != null) {
                    log.debug("in fetchMessage processing " + groupMsgContainerId);

                    GroupedMessageContainer groupedMsgContainerDocument = groupContainerStore.get(groupMsgContainerId);
                    String messageId = groupedMsgContainerDocument.getFirstMessageId();
                    msgInProcessByClientSet.add(messageId);
                    messageWrapper = msgStoreForGroups.get(messageId);
                    log.debug("sending message with expiry time " + EXPIRY_TIME);
                    sendJMSMessage(EXPIRY_TIME, collectionName, messageId, groupMsgContainerId);
                    // log.debug("fetchMessage message wrapper is " + messageWrapper + " and message is " + messageWrapper != null ? messageWrapper.getMessage() : "");

                    //msgStoreForGroups
                    builder = Response.status(Response.Status.FOUND);

                    builder.entity(getMessageResponse(messageWrapper));
                    // addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
                }else{
                    builder = Response.status(Response.Status.NO_CONTENT);
                    builder.entity(produceErrorMessage(Response.Status.NO_CONTENT.getStatusCode(),Response.Status.NO_CONTENT.getStatusCode(),"No message available for processing"));
                }
            } catch (Exception e) {
                if (groupMsgContainerId != null) {
                    e.printStackTrace();
                    log.error("error " + e);
                    builder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
                    builder.entity(produceErrorMessage(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),"Please contact admin, server error"));
                }

            }
        } else {
            builder = noSuchCollectionResponse();
            return builder.build();
        }
        // builder = Response.status(Response.Status.OK);
        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return builder.build();
    }

    private Map<String, Object> getMessageResponse(MessageWrapper messageWrapper) {
       // Set<Object> responseObject = new HashSet<>();
        Map<String, Object> responseObject = new HashMap<>();
        responseObject.put("meta",messageWrapper.getMeta());
        responseObject.put("message",messageWrapper.getMessage());
        return responseObject;
    }

    private Queue<String> getQueue(String key) {
        return collectionAndQueueMapper.get(key);
    }


    @Path("/message/fifo/group/namespace/fft/collection/{collectionName}")
    @PUT

    @ApiOperation(value = "Update Message by id",
            notes = "Update Message by id")
    @ApiResponses(value = {
            @ApiResponse(code = 404,
                    message = "For empty queue"),
            @ApiResponse(code = 200,
                    message = "Success"
            )})
    public Response updateMessage(@PathParam("collectionName") String collectionName,@QueryParam(Constants.MSG_ID) String messageId, @RequestBody Map<String, Object> body) {
        Response.ResponseBuilder builder =Response.status(Response.Status.OK);
        Map<String, Object> messageDocument = MapUtils.isNotEmpty(body)?(Map<String, Object>) body.get("message"):null ;
        if(MapUtils.isNotEmpty(body) && body.size()==1 && MapUtils.isNotEmpty(messageDocument) && messageDocument.size()<=2 ) {

            JsonParser jsonParser = new JsonParser();
            JsonElement data;
            Gson gson = new GsonBuilder().create();
            try {

                data = jsonParser.parse(gson.toJson(messageDocument.get(DATA)));
                log.debug("data after parse is " + data);

            } catch (Exception e) {
                log.error("Error while parsing data " + e.getMessage());
                Response.ResponseBuilder responseBuilder = dataParsingException(e);
                return responseBuilder.build();
            }
            // Map<String,Object> localMsgDocument;
            MessageWrapper messageWrapper;
            String groupIdentifier = messageDocument.get(GROUP) instanceof String ? messageDocument.get(GROUP).toString() : null;
            if (groupIdentifier != null && groupIdentifier.length() > 0 && !data.isJsonNull()) {
                if ((collectionName != null && collectionName.length() > 0 && collectionAndQueueMapper.containsKey(collectionName))) {
                    messageWrapper = msgStoreForGroups.get(messageId);
                    if (messageWrapper != null) {
                        messageWrapper.setMessage(messageDocument);

                        messageWrapper.getMeta().put(Constants.UPDATED_TIME,Util.generateISO8601(DateTime.now()));
                        persistMessage(messageWrapper.getMessageId(), messageWrapper);
                        log.debug("message updated " + messageId);
                        //builder = Response.status(Response.Status.OK);
                        Map<String, Object> responseObject = new HashMap<>();
                        responseObject.put("meta",messageWrapper.getMeta());
                        responseObject.put("action","Message Updated");
                        builder.entity(responseObject);

                    } else {

                        builder = Response.status(Response.Status.NOT_FOUND);
                        builder.entity(produceErrorMessage(Response.Status.NOT_FOUND.getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), "Message not found"));
                        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
                        return builder.build();
                    }
                }else {
                    builder = noSuchCollectionResponse();
                }
            }else {
                builder = invalidPayload();
            }
        }else{
            builder=invalidPayload();
        }
        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return builder.build();

    }




    @Path("/message/fifo/group/namespace/fft/collection/{collectionName}")
    @DELETE
    @ApiOperation(value = "Delete Message by id",
            notes = "Delete Message by id")
    @ApiResponses(value = {
            @ApiResponse(code = 404,
                    message = "For empty queue"),
            @ApiResponse(code = 200,
                    message = "Success"
            ),
            @ApiResponse(code = 400,
                    message = "Bad Request, response not sent in agreed time"
            )})
    public Response deleteMessage(@QueryParam(Constants.MSG_ID) String messageId, @PathParam("collectionName") String collectionName) {
        Response.ResponseBuilder builder;
        MessageWrapper messageWrapper = msgStoreForGroups.get(messageId);
        if ((collectionName != null && collectionName.length() > 0 && collectionAndQueueMapper.containsKey(collectionName))) {
            //fetch the group document, remove this message as it is proccessed by client, then update the document back if it still
            //contains message, other wise remove it.
            if (messageId != null && msgInProcessByClientSet.contains(messageId)) {
                //TODO need to check impact of concurrent access, as new message might arrive and delete is trying to remove it
                GroupedMessageContainer groupedMessageContainer = groupContainerStore.get(messageWrapper.getContainerReference());
                groupedMessageContainer.removeMessage(messageId);
                String groupMsgContainerId = groupedMessageContainer.getDocId();
                //remove group id from queueContentChecker, In-progress queue will self clean it.
                queueContentChecker.remove(groupMsgContainerId);
                msgInProcessByClientSet.remove(messageId);
                //check if there are no more pending message remove the group
                if (!groupedMessageContainer.isContainerMessageEmpty()) {

                    groupContainerStore.put(groupMsgContainerId, groupedMessageContainer);
                    //if there are pending message then this group needs to re-queued, add it to queue
                    // and update content checker again. As there might be multiple threads at client end
                    // check the document was queued or not.
                    if (!isGroupDocumentQueued(groupMsgContainerId)) {
                        //add to queue
                        collectionAndQueueMapper.get(collectionName).add(groupMsgContainerId);
                        log.debug("queue status after delete is " + collectionAndQueueMapper.get(collectionName));
                        queueContentChecker.add(groupMsgContainerId);
                    }
                } else {
                    log.debug("removing container");
                    groupContainerStore.remove(groupMsgContainerId);
                }
                // delete message from message store
                msgStoreForGroups.remove(messageId);
                log.debug("message deleted " + messageId);
                builder = Response.status(Response.Status.OK);
                Map<String, Object> responseObject = new HashMap<>();
                responseObject.put("meta",messageWrapper.getMeta());
                responseObject.put("action","Message Deleted");
                builder.entity(responseObject);
            } else {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(produceErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getStatusCode(),"Bad Request,Either response not sent in agreed time or invalid msgId."));
            }
        } else {
            builder = noSuchCollectionResponse();
            return builder.build();
        }
        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return builder.build();
    }

    private Response.ResponseBuilder noSuchCollectionResponse() {
        Response.ResponseBuilder builder;
        builder = Response.status(Response.Status.NOT_FOUND);
        builder.entity(produceErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),Response.Status.NOT_FOUND.getStatusCode(),"Collection name not identified"));
        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return builder;
    }

    private Response.ResponseBuilder dataParsingException(Exception e) {
        Response.ResponseBuilder builder;
        builder = Response.status(Response.Status.BAD_REQUEST);
        builder.entity(produceErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getStatusCode(),"data encoding error " + e.getMessage()));
        addCharacterEncoding(builder, MediaType.APPLICATION_JSON, Charsets.UTF_8);
        return builder;
    }

    private void sendJMSMessage(long expiryTime, String collectionName, String msgId, String groupMsgContainerId) {
        Connection connection = null;
        Session session = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            String text = collectionName + "-" + msgId + "-" + groupMsgContainerId;
            TextMessage message = session.createTextMessage(text);

            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, expiryTime);
            message.setJMSCorrelationID(groupMsgContainerId);
            jmsTemplate.setExplicitQosEnabled(true);
            jmsTemplate.setTimeToLive(expiryTime);
            jmsTemplate.convertAndSend(msgInProgressQueue, message);
            log.debug("**** Message Sent ******* : " + groupMsgContainerId);
        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            try {
                session.close();
            } catch (Exception e) {

            }
            try {
                connection.close();
            } catch (Exception e) {

            }
        }

    }

    /*@Path("/message/expiry/")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "This is temporary and should not be used for development", notes = "This is temporary, this will be removed and should not be used for development"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404,
                    message = "Not found"),
            @ApiResponse(code = 200,
                    message = "Success"
            )})*/
    public Response updateExpiryTime(@QueryParam("expiryTime") long expiryTime) {
        Response.ResponseBuilder builder;
        try {
            this.EXPIRY_TIME = expiryTime;
            log.debug("Expriy time set to " + EXPIRY_TIME);
            builder = Response.status(Response.Status.OK);
            String msg = "This is temporary, this will be removed and should not be used for development.";
            builder.entity(produceMessage(msg));
        } catch (Exception e) {
            e.printStackTrace();
            builder = Response.status(Response.Status.SERVICE_UNAVAILABLE);

        }
        return builder.build();
    }

    /**
     * if the group container still has message then re-queue it otherwise ignore it
     *
     * @param groupMsgContainerId
     * @param collectionName
     */
    public void handleTimeout(String msgId, String collectionName, String groupMsgContainerId) {
        GroupedMessageContainer groupedMessageContainer = groupContainerStore.get(groupMsgContainerId);
        log.debug(" groupedMessageContainer is " + groupedMessageContainer);
        if (msgInProcessByClientSet.contains(msgId)) {
            //if null do nothing, as that would mean container doesn't exist anymore.
            if (groupedMessageContainer != null && !groupedMessageContainer.isContainerMessageEmpty()) {
                log.debug(" Before reprocessing queue status is " + collectionAndQueueMapper.get(collectionName));
                msgInProcessByClientSet.remove(msgId);
                //TODO RACE CONDITION this is kind of backdoor entry need to check this condition, as inserting in queue without checking queueContentChecker
                collectionAndQueueMapper.get(collectionName).add(groupMsgContainerId);

                log.debug(" After reprocessing queue status is " + collectionAndQueueMapper.get(collectionName));
                if (!isGroupDocumentQueued(groupMsgContainerId)) {

                    log.debug(" Document was not marked as queued in queueContentChecker, re-marking it");
                    queueContentChecker.add(groupMsgContainerId);
                }
            }
        } else {
            log.debug("message was already deleted, no action taken");
        }
    }


    private boolean isGroupDocumentQueued(String groupMsgContainerId) {
        log.debug("isGroupDocumentQueued " + queueContentChecker.contains(groupMsgContainerId));
        //return true if already queued else false
        return queueContentChecker.contains(groupMsgContainerId);
    }

    private String produceMessage(String message) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject responseElement = new JsonObject();
        responseElement.addProperty("message", message);
        return gson.toJson(responseElement);
    }
    private String produceErrorMessage(int status, int code, String message) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject errorElement = new JsonObject();
        errorElement.addProperty("status", status);
        errorElement.addProperty("error", code);
        errorElement.addProperty("message", message);
        return gson.toJson(errorElement);
    }
}