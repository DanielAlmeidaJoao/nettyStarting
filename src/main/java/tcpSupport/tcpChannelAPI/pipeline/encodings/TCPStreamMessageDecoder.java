package tcpSupport.tcpChannelAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpChannelAPI.metrics.TCPStreamMetrics;

import java.util.List;

public class TCPStreamMessageDecoder extends ByteToMessageDecoder {

    private final TCPStreamMetrics metrics;
    public static final String NAME="TCPStreamMessageDecoder";
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;

    public TCPStreamMessageDecoder(TCPStreamMetrics metrics, StreamingNettyConsumer consumer) {
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
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelOutputStream);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
