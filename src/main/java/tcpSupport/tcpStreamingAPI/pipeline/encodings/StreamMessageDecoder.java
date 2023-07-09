package tcpSupport.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;

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
        int available = in.readableBytes();
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+available);
        }

        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedSlice());
        in.readerIndex(in.readableBytes());
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(), babelOutputStream);
    }
}
