package tcpSupport.tcpChannelAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

import java.util.List;

//@ChannelHandler.Sharable
public class TCPCustomHandshakeHandler extends ByteToMessageDecoder {

    public static final String NAME ="CHSHAKE_HANDLER";
    private final StreamingNettyConsumer consumer;
    private int len;
    public TCPCustomHandshakeHandler(StreamingNettyConsumer consumer){
        this.consumer = consumer;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
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
        if(TransmissionType.STRUCTURED_MESSAGE==handShakeMessage.type){
            ctx.channel().pipeline().addLast(TCPDelimitedMessageDecoder.NAME,new TCPDelimitedMessageDecoder(consumer));
        }else{
            ctx.channel().pipeline().addLast(TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(consumer));
        }
        consumer.onChannelActive(ctx.channel(),handShakeMessage, handShakeMessage.type,len);
        ctx.channel().pipeline().remove(TCPCustomHandshakeHandler.NAME);
    }
}
