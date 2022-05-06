package com.group29.mobileoffloading.DataModels;

import java.io.Serializable;

public class NodeDataPayload implements Serializable {
    private String tag;
    private Object data;

    public String getTag() {
        return tag;
    }

    public NodeDataPayload setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public Object getData() {
        return data;
    }

    public NodeDataPayload setData(Object data) {
        this.data = data;
        return this;
    }
}