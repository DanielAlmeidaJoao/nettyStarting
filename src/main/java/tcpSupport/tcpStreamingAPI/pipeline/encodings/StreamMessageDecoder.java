package tcpSupport.tcpStreamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.commons.lang3.tuple.Pair;
import tcpSupport.tcpStreamingAPI.channel.TCPNettyConsumer;
import tcpSupport.tcpStreamingAPI.metrics.TCPSConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPMetrics;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.List;

public class StreamMessageDecoder extends ByteToMessageDecoder {

    private final TCPMetrics metrics;
    public static final String NAME="StreamMessageDecoder";
    public final TCPNettyConsumer consumer;
    public final TransmissionType type;
    public final Pair<InetSocketAddress,String> identification;

    public StreamMessageDecoder(TCPMetrics metrics, TCPNettyConsumer consumer, Pair<InetSocketAddress, String> connectionId) {
        this.metrics = metrics;
        this.consumer = consumer;
        type = TransmissionType.UNSTRUCTURED_STREAM;
        identification = connectionId;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        byte[] bytes = new byte[in.readableBytes()];
        if(metrics!=null){
            TCPSConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
            metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+bytes.length);
        }
        in.readBytes(bytes);
        consumer.onChannelRead(identification,bytes,type);
    }
}
