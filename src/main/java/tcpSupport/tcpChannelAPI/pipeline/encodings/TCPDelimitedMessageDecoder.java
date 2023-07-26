package tcpSupport.tcpChannelAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;

import java.util.List;

public class TCPDelimitedMessageDecoder extends ByteToMessageDecoder {
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;
    public static final String NAME = "TCPDelimitedMessageDecoder";

    public TCPDelimitedMessageDecoder(StreamingNettyConsumer consumer) {
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
        /**
        byte [] data = new byte[length];
        in.readBytes(data);
        **/

        consumer.onChannelMessageRead(ctx.channel().id().asShortText(),in);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        //cause.printStackTrace();
        ctx.close();
    }
}
