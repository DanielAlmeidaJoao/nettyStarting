package org.tcpStreamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CustomChannelHandler.class);

    @Getter
    private final StreamingNettyConsumer consumer;

    public CustomChannelHandler(StreamingNettyConsumer consumer){
        this.consumer = consumer;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getConsumer().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        consumer.onChannelInactive(ctx.channel().id().asShortText());
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        consumer.onConnectionFailed(ctx.channel().id().asShortText(),cause);
        cause.printStackTrace();
        ctx.close();
    }

}
