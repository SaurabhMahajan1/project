package com.centrica.bg.pulse.fifomessaging.entity;

import com.google.gson.JsonObject;

/**
 * Created by !basotim1 on 29/09/2016.
 */
public class Message {
    String group;
    JsonObject data;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    private boolean isValidGroupEntity(){

        return group!=null?!group.isEmpty():false;
    }
}
