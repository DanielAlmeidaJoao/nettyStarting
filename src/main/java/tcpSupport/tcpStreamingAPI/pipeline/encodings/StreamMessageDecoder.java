package tcpSupport.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.streamUtils.BabelInBytesWrapper;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
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
        //byte[] bytes = new byte[in.readableBytes()];
        int available = in.readableBytes();
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+available);
        }
        //in.readBytes(bytes);
        BabelInBytesWrapper babelInBytesWrapper = new BabelInBytesWrapper(in);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelInBytesWrapper);
    }
}
