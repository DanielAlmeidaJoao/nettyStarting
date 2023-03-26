package org.streamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.utils.FactoryMethods;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    private HandShakeMessage handshakeData;
    public StreamSenderHandler(HandShakeMessage handshakeData, InNettyChannelListener inNettyChannelListener){
        super(inNettyChannelListener);
        this.handshakeData = handshakeData;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        byte [] data = FactoryMethods.g.toJson(handshakeData).getBytes();
        ByteBuf tmp = Unpooled.buffer(data.length+4);
        tmp.writeInt(data.length);
        tmp.writeBytes(data);

        ctx.writeAndFlush(tmp);
        getConsumer().onChannelActive(ctx.channel(),handshakeData);
        handshakeData=null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        System.out.println("TRIGGER AN ERROR!!!");
        cause.printStackTrace();
        ctx.close();
    }
}