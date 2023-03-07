package org.streamingAPI.handlerFunctions.receiver;

//END OF STREAMING FUNCTION
@FunctionalInterface
public interface ChannelInactiveHandler {
    public void execute(String id);
}
