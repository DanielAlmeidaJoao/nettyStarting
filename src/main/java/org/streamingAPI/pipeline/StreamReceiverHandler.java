package org.streamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    public StreamReceiverHandler(InNettyChannelListener inNettyChannelListener){
        super(inNettyChannelListener);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getConsumer().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
}
