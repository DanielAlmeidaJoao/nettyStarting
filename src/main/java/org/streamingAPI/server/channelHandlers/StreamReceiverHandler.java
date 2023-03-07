package org.streamingAPI.server.channelHandlers;

import org.streamingAPI.handlerFunctions.receiver.HandlerFunctions;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {

    public StreamReceiverHandler(HandlerFunctions handlerFunctions){
        super(handlerFunctions);
    }
}
