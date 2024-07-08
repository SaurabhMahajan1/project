package com.centrica.bg.pulse.fifomessaging.entity;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by !basotim1 on 27/08/2016.
 */
public class MessageWrapper{
    Map<String, Object> message;
    Map<String, Object> meta;
    String containerReference;

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public String getMessageId(){
        return getMeta().get(Constants.MSG_ID).toString();
    }

    public String getContainerReference() {
        return containerReference;
    }

    public void setContainerReference(String containerReference) {
        this.containerReference = containerReference;
    }

    /* @Override
    public int compareTo(MessageWrapper o) {
        DateTime timestamp1 = new DateTime((o.getMeta()).get(Constants.CREATE_TIMESTAMP), DateTimeZone.UTC);
        DateTime timestamp2 = new DateTime((o.getMeta()).get(Constants.CREATE_TIMESTAMP), DateTimeZone.UTC);
        return timestamp1.compareTo(timestamp2);
    }*/
}
