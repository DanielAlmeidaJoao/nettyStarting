package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface OpenConnectionFailedHandler {
    void execute(String channelId,Throwable cause);
}
