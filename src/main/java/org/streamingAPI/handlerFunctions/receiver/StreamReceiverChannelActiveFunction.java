package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverChannelActiveFunction {
    public void execute(String channelId);
}
