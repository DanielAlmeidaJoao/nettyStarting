package org.streamingAPI.server.channelHandlers;

import org.streamingAPI.server.listeners.InChannelListener;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {

    public StreamReceiverHandler(InChannelListener inChannelListener){
        super(inChannelListener);
    }
}
