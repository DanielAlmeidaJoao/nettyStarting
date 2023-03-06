package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverFunction {
    public void execute(String id, byte [] data);
}
