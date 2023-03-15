package org.streamingAPI.client.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.server.channelHandlers.CustomChannelHandler;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    byte [] controlData;
    public StreamSenderHandler(byte [] handshakeData, InNettyChannelListener inNettyChannelListener, boolean readDelimited){
        super(inNettyChannelListener);
        this.controlData = handshakeData;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf tmp = Unpooled.buffer();
        if(controlData==null){
            tmp.writeInt(-1);
            System.out.println("--1");
        }else {
            tmp.writeInt(controlData.length);
            tmp.writeBytes(controlData);
            System.out.println("--1 "+controlData.length);

        }
        ctx.writeAndFlush(tmp);
        controlData=null;
        System.out.println("SENT CONTROL ");
        Channel channel = ctx.channel(); // get the channel from somewhere
        getInNettyChannelListener().onChannelActive(ctx.channel(),null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        System.out.println("TRIGGER AN ERROR!!!");
        cause.printStackTrace();
        ctx.close();
    }
}