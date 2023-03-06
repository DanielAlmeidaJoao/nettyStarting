package org.streamingAPI.client;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends ChannelHandlerAdapter {

    public StreamSenderHandler(){}
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}