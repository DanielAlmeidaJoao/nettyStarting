package org.streamingAPI.server;

import org.streamingAPI.handlerFunctions.receiver.HandlerFunctions;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {

    public StreamReceiverHandler(HandlerFunctions handlerFunctions){
        super(handlerFunctions);
    }
}
