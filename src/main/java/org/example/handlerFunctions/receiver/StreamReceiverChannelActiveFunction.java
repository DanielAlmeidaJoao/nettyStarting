package org.example.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverChannelActiveFunction {
    public void execute(String channelId);
}
