package org.tcpStreamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    private final TCPStreamMetrics metrics;
    public StreamReceiverHandler(TCPStreamMetrics metrics, StreamingNettyConsumer consumer, String connectionId){
        super(consumer, null);
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }

}
