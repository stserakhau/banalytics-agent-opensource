package com.banalytics.box.api.integration;

public interface IMessage {
    String getType();

    String toJson() throws Exception;
}
