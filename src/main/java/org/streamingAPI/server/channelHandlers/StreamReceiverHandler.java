package org.streamingAPI.server.channelHandlers;

import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.server.listeners.InNettyChannelListener;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    public StreamReceiverHandler(InNettyChannelListener inNettyChannelListener){
        super(inNettyChannelListener);
    }
    /**
     * Used when the sender specified the length of the data in the first bytes of the array
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getInNettyChannelListener().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
}
