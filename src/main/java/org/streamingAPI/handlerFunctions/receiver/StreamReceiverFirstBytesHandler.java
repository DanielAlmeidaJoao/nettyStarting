package org.streamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverFirstBytesHandler {
    public void execute(byte [] data);
}
