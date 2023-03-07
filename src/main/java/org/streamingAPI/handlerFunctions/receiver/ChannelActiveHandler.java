package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface ChannelActiveHandler {
    public void execute(String channelId);
}
