package org.example.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends ChannelHandlerAdapter {
    private byte [] data;

    public StreamSenderHandler(byte [] handshakeData){
        this.data = handshakeData;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if(data==null){
            ctx.writeAndFlush(Unpooled.copyInt(0));
        }else {
            ctx.writeAndFlush(Unpooled.copiedBuffer(data));
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}