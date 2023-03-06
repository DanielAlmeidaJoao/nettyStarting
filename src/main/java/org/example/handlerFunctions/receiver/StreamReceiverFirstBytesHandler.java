package org.example.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverFirstBytesHandler {
    public void execute(byte [] data);
}
