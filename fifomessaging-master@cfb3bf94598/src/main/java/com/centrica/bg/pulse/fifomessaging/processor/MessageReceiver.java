package com.centrica.bg.pulse.fifomessaging.processor;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import com.centrica.bg.pulse.fifomessaging.http.rest.DocumentResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * Created by !basotim1 on 05/09/2016.
 */
@Component
public class MessageReceiver {

    private Log log = LogFactory.getLog(MessageReceiver.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    DocumentResource documentResource;

    /*@JmsListener(destination = "ActiveMQ.DLQ")
    public void receiveMessage(Object message) {
        TextMessage textMessage = (TextMessage)message;
        try {
            if(textMessage.getJMSCorrelationID().contains("wmis") || textMessage.getJMSCorrelationID().contains("TF")) {
                System.out.println("Received and processing <" + textMessage.getJMSCorrelationID() + ">");
                String [] msgDetails = textMessage.getText().split("-");
                documentResource.handleTimeout(msgDetails[1],msgDetails[0],msgDetails[2]);
            }else{
                System.out.println("**********IGNORED*********** <" + textMessage.getJMSCorrelationID() + ">");
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }*/

    @JmsListener(destination = "ActiveMQ.DLQ")
    public void receiveMessage(Object message) {
        TextMessage textMessage = (TextMessage)message;
        try {
            if(isValidMessageType(textMessage)) {
                log.debug("############### Message Received and processing timeout for  " + textMessage.getJMSCorrelationID()+" ###################");
                log.debug("message details "+message);
                String [] msgDetails = textMessage.getText().split("-");
                documentResource.handleTimeout(msgDetails[1],msgDetails[0],msgDetails[2]);
                log.debug("############### Message PROCESSED Successfully #############");
            }else{
                log.debug("############### Message IGNORED ###############" + textMessage.getJMSCorrelationID());
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
    private boolean isValidMessageType(TextMessage textMessage) throws JMSException {
        boolean isValid = false;
        if(textMessage.getJMSCorrelationID().contains(Constants.WMISQ_COLLECTION_MAPPER) || textMessage.getJMSCorrelationID().contains(Constants.TF_COLLECTION_MAPPER) ||
                textMessage.getJMSCorrelationID().contains(Constants.PI_COLLECTION_MAPPER) || textMessage.getJMSCorrelationID().contains(Constants.CB_COLLECTION_MAPPER)) {
            isValid=true;
        }
        return isValid;
    }
}

