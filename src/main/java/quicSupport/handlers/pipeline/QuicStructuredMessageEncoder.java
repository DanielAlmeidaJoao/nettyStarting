package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.DelimitedMessageWrapper;
import quicSupport.utils.enums.TransmissionType;

public class QuicStructuredMessageEncoder extends MessageToByteEncoder<DelimitedMessageWrapper> {
    public static final String HANDLER_NAME="QuicMessageEncoder";
    public final TransmissionType type;

    public QuicStructuredMessageEncoder(){
        type = TransmissionType.STRUCTURED_MESSAGE;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, DelimitedMessageWrapper message, ByteBuf byteBuf) {
        byteBuf.writeInt(message.len);
        byteBuf.writeByte(message.msgCode);
        byteBuf.writeBytes(message.data);
    }

}
