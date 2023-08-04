package tcpSupport.tcpChannelAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

//@ChannelHandler.Sharable
public class TCPCustomHandshakeHandler extends AbstractMessageDecoderHandler {

    public static final String NAME ="CHSHAKE_HANDLER";
    private int len;
    public TCPCustomHandshakeHandler(StreamingNettyConsumer consumer){
        super(consumer);
    }

    @Override
    public boolean handleReceivedMessage(ChannelHandlerContext ctx, ByteBuf in, int len) {
        byte [] controlData = new byte[len];
        in.readBytes(controlData,0,len);
        in.discardReadBytes();
        String gg = new String(controlData);
        HandShakeMessage handShakeMessage = TCPStreamUtils.g.fromJson(gg, HandShakeMessage.class);
        if(TransmissionType.STRUCTURED_MESSAGE==handShakeMessage.type){
            ctx.channel().pipeline().addLast(TCPDelimitedMessageDecoder.NAME,new TCPDelimitedMessageDecoder(consumer));
        }else{
            ctx.channel().pipeline().addLast(TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(consumer));
        }
        consumer.onChannelActive(ctx.channel(),handShakeMessage, handShakeMessage.type,len);
        ctx.channel().pipeline().remove(TCPCustomHandshakeHandler.NAME);
        return false;
    }
}
