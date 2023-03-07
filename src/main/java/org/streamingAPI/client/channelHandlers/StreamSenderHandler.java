package org.streamingAPI.client.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.receiver.HandlerFunctions;
import org.streamingAPI.server.channelHandlers.CustomChannelHandler;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    byte [] controlData;
    public StreamSenderHandler(byte [] handshakeData, HandlerFunctions handlerFunctions){
        super(handlerFunctions);
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
}