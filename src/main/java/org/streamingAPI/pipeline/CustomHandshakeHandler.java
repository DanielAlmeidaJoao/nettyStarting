package org.streamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.streamingAPI.channel.StreamingNettyConsumer;
import org.streamingAPI.metrics.TCPStreamConnectionMetrics;
import org.streamingAPI.metrics.TCPStreamMetrics;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;
import org.streamingAPI.utils.FactoryMethods;

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
        byte [] controlData = new byte[len];
        in.readBytes(controlData,0,len);
        String gg = new String(controlData);
        HandShakeMessage handShakeMessage = FactoryMethods.g.fromJson(gg, HandShakeMessage.class);
        consumer.onChannelActive(ctx.channel(),handShakeMessage);
        if(metrics!=null){
            TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
            metrics1.setReceivedControlBytes(metrics1.getReceivedControlBytes()+len);
            metrics1.setReceivedControlMessages(metrics1.getReceivedControlMessages()+1);
        }
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
}
