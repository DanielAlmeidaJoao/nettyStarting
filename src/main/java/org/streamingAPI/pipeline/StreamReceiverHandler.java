package org.streamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.metrics.TCPStreamMetrics;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    private final TCPStreamMetrics metrics;

    public StreamReceiverHandler(InNettyChannelListener inNettyChannelListener, TCPStreamMetrics metrics){
        super(inNettyChannelListener);
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getConsumer().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
}
