package org.streamingAPI.handlerFunctions.receiver;

//END OF STREAMING FUNCTION
@FunctionalInterface
public interface StreamReceiverEOSFunction {
    public void execute(String id);
}
