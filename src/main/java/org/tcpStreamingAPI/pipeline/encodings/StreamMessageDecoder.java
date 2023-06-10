package org.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.util.List;

public class StreamMessageDecoder extends ByteToMessageDecoder {

    private final TCPStreamMetrics metrics;
    public static final String NAME="StreamMessageDecoder";
    public final StreamingNettyConsumer consumer;
    public final ConnectionOrStreamType type;
    long received = 0 ;

    public StreamMessageDecoder(TCPStreamMetrics metrics, StreamingNettyConsumer consumer) {
        this.metrics = metrics;
        this.consumer = consumer;
        type = ConnectionOrStreamType.UNSTRUCTURED_STREAM;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+bytes.length);
        }
        received += bytes.length;
        consumer.onChannelRead(ctx.channel().id().asShortText(),bytes,type);
    }
}
