package org.streamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.channel.StreamingNettyConsumer;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.metrics.TCPStreamMetrics;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    private final TCPStreamMetrics metrics;
    public StreamReceiverHandler(TCPStreamMetrics metrics, StreamingNettyConsumer consumer){
        super(consumer);
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }

}
