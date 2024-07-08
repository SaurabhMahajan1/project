package com.centrica.bg.pulse.fifomessaging.constant;

/**
 * Constants
 *
 *
 */
public interface Constants {
    String RESOURCE_PACKAGE = "com.centrica.bg.pulse.fifomessaging";
    String HTTP = "http";
    String HTTPS = "https";
    String REST_PACKAGE = "com.centrica.bg.pulse.fifomessaging.http.rest";
    String BASE_PATH = "/fifomessaging/api";
    String WEB_CONTEXT_ROOT = "/fifomessaging";
    String API_VERSION = "1.0.0";
    String TITLE = "fifomessaging";
    String DESCRIPTION = "FIFO Messaging API with ordered group support";

    String CREATE_TIMESTAMP = "createTimestamp";
    String MSG_ID = "messageId";
    String WMISQ_COLLECTION_MAPPER = "WMIS";
    String TF_COLLECTION_MAPPER = "TF";
    String PI_COLLECTION_MAPPER = "PI";
    String CB_COLLECTION_MAPPER = "CB";

    String UPDATED_TIME = "updatedTimestamp";
    String ISO_8601_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss:SSSZ";
    String FILE_NAME_DATE_PATTERN = "yyyyMMddHHmmssSSS";
}
