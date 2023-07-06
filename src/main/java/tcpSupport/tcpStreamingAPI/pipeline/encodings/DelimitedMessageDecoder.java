package tcpSupport.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import quicSupport.utils.enums.TransmissionType;

import java.util.List;

public class DelimitedMessageDecoder extends ByteToMessageDecoder {
    private final TCPStreamMetrics metrics;
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;
    public static final String NAME = "DelimitedMessageDecoder";

    public DelimitedMessageDecoder(TCPStreamMetrics metrics, StreamingNettyConsumer consumer) {
        this.metrics = metrics;
        this.consumer=consumer;
        type = TransmissionType.STRUCTURED_MESSAGE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        if(in.readableBytes()<4){
            return;
        }
        in.markReaderIndex();
        int length = in.readInt();

        if(in.readableBytes()<length){
            in.resetReaderIndex();
            return;
        }
        byte [] data = new byte[length];
        in.readBytes(data);
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+length+4);
        }
        consumer.onChannelMessageRead(ctx.channel().id().asShortText(),data);
    }
}
