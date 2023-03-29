package org.streamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.channel.StreamingNettyConsumer;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CustomChannelHandler.class);

    @Getter
    private StreamingNettyConsumer consumer;

    public CustomChannelHandler(StreamingNettyConsumer consumer){
        this.consumer = consumer;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getConsumer().channelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        consumer.channelInactive(ctx.channel().id().asShortText());
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        consumer.onConnectionFailed(ctx.channel().id().asShortText(),cause);
        cause.printStackTrace();
        ctx.close();
    }

}
