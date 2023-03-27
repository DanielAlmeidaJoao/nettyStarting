package org.streamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.streamingAPI.metrics.TCPStreamMetrics;

import java.util.List;

public class StreamMessageDecoder extends ByteToMessageDecoder {

    private final TCPStreamMetrics metrics;

    public StreamMessageDecoder(TCPStreamMetrics metrics) {

        this.metrics = metrics;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        out.add(bytes);
    }
}
