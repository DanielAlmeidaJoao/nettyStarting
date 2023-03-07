package org.streamingAPI.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends ChannelHandlerAdapter {

    byte [] controlData;
    public StreamSenderHandler(byte [] handshakeData){
        this.controlData = handshakeData;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf tmp = Unpooled.buffer();
        if(controlData==null){
            tmp.writeInt(-1);
        }else {
            tmp.writeInt(controlData.length);
            tmp.writeBytes(controlData);
        }
        ctx.writeAndFlush(tmp);
        controlData=null;
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}