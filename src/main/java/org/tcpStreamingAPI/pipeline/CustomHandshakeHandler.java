package org.tcpStreamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import org.tcpStreamingAPI.pipeline.encodings.StreamMessageDecoder;
import org.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";
    private final TCPStreamMetrics metrics;
    private final StreamingNettyConsumer consumer;
    private int len;
    public final String connectionId;
    public CustomHandshakeHandler(TCPStreamMetrics metrics, StreamingNettyConsumer consumer, String connectionId){
        this.metrics = metrics;
        this.consumer = consumer;
        this.connectionId = connectionId;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnknownHostException {
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
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
        byte [] controlData = new byte[len];
        in.readBytes(controlData,0,len);
        String gg = new String(controlData);
        HandShakeMessage handShakeMessage = TCPStreamUtils.g.fromJson(gg, HandShakeMessage.class);
        Pair<InetSocketAddress,String> identification = Pair.of(handShakeMessage.getAddress(),connectionId);
        if(TransmissionType.UNSTRUCTURED_STREAM == handShakeMessage.type){
            //ctx.channel().pipeline().replace(DelimitedMessageDecoder.NAME, StreamMessageDecoder.NAME,new StreamMessageDecoder(metrics, consumer, connectionId));
            ctx.channel().pipeline().addLast(StreamMessageDecoder.NAME,new StreamMessageDecoder(metrics,consumer,identification));
        }else{
            ctx.channel().pipeline().addLast(DelimitedMessageDecoder.NAME,new DelimitedMessageDecoder(metrics,consumer,identification));
        }
        consumer.onChannelActive(ctx.channel(),handShakeMessage,handShakeMessage.type,identification);
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
}
