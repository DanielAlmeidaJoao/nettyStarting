package tcpSupport.tcpStreamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.StreamMessageDecoder;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.TransmissionType;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";
    private final TCPStreamMetrics metrics;
    private final StreamingNettyConsumer consumer;
    private int len;
    public CustomHandshakeHandler(TCPStreamMetrics metrics, StreamingNettyConsumer consumer){
        this.metrics = metrics;
        this.consumer = consumer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        if(in.readableBytes()<4){
            return;
        }
        in.markReaderIndex();
        len = in.readInt();
        if(in.readableBytes()<len){
            in.resetReaderIndex();
            return;
        }

        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedControlBytes(metrics1.getReceivedControlBytes()+len);
            metrics1.setReceivedControlMessages(metrics1.getReceivedControlMessages()+1);
        }
        byte [] controlData = new byte[len];
        in.readBytes(controlData,0,len);
        String gg = new String(controlData);
        HandShakeMessage handShakeMessage = TCPStreamUtils.g.fromJson(gg, HandShakeMessage.class);
        if(TransmissionType.UNSTRUCTURED_STREAM == handShakeMessage.type){
            ctx.channel().pipeline().replace(DelimitedMessageDecoder.NAME, StreamMessageDecoder.NAME,new StreamMessageDecoder(metrics, consumer));
        }
        consumer.onChannelActive(ctx.channel(),handShakeMessage, handShakeMessage.type);
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
}
