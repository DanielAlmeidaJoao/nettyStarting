package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface ChannelActiveReadHandler {
    public void execute(byte [] data);
}
