package org.example.handlerFunctions.receiver;

@FunctionalInterface
public interface StreamReceiverFunction {
    public void execute(String id, byte [] data);
}
