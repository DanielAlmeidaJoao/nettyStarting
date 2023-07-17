package tcpSupport.tcpChannelAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

//@ChannelHandler.Sharable
public class TCPCustomHandshakeHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";
    private final StreamingNettyConsumer consumer;
    private int len;
    public TCPCustomHandshakeHandler(StreamingNettyConsumer consumer){
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
        in.discardReadBytes();
        String gg = new String(controlData);
        HandShakeMessage handShakeMessage = TCPStreamUtils.g.fromJson(gg, HandShakeMessage.class);
        if(TransmissionType.UNSTRUCTURED_STREAM == handShakeMessage.type){
            ctx.channel().pipeline().replace(TCPDelimitedMessageDecoder.NAME, TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(consumer));
        }
        consumer.onChannelActive(ctx.channel(),handShakeMessage, handShakeMessage.type,len);
        ctx.channel().pipeline().remove(TCPCustomHandshakeHandler.NAME);
        ctx.fireChannelRead(in);
    }
}
