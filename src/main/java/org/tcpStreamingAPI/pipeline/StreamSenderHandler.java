package org.tcpStreamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.utils.FactoryMethods;

import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    private HandShakeMessage handshakeData;
    private final TCPStreamMetrics metrics;

    public StreamSenderHandler(HandShakeMessage handshakeData, StreamingNettyConsumer consumer, TCPStreamMetrics metrics){
        super(consumer);
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