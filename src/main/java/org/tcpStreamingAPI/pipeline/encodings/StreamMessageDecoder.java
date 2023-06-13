package org.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import quicSupport.utils.enums.TransmissionType;

import java.util.List;

public class StreamMessageDecoder extends ByteToMessageDecoder {

    private final TCPStreamMetrics metrics;
    public static final String NAME="StreamMessageDecoder";
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;

    public StreamMessageDecoder(TCPStreamMetrics metrics, StreamingNettyConsumer consumer) {
        this.metrics = metrics;
        this.consumer = consumer;
        type = TransmissionType.UNSTRUCTURED_STREAM;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        byte[] bytes = new byte[in.readableBytes()];
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+bytes.length);
        }
        in.readBytes(bytes);
        consumer.onChannelRead(ctx.channel().id().asShortText(),bytes,type);
    }
}
