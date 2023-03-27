package org.streamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.metrics.TCPStreamConnectionMetrics;
import org.streamingAPI.metrics.TCPStreamMetrics;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.utils.FactoryMethods;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    private HandShakeMessage handshakeData;
    private final TCPStreamMetrics metrics;

    public StreamSenderHandler(HandShakeMessage handshakeData, InNettyChannelListener inNettyChannelListener, TCPStreamMetrics metrics){
        super(inNettyChannelListener);
        this.handshakeData = handshakeData;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws UnknownHostException {
        byte [] data = FactoryMethods.g.toJson(handshakeData).getBytes();
        ByteBuf tmp = Unpooled.buffer(data.length+4);
        tmp.writeInt(data.length);
        tmp.writeBytes(data);
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
        ctx.writeAndFlush(tmp).addListener(future -> {
            if(future.isSuccess()){
                if(metrics!=null){
                    TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
                    metrics1.setSentControlBytes(metrics1.getSentControlBytes()+data.length+4);
                    metrics1.setSentControlMessages(metrics1.getSentControlMessages()+1);
                }
            }else{
                future.cause().printStackTrace();
                ctx.channel().close();
            }
        });
        getConsumer().onChannelActive(ctx.channel(),null);
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