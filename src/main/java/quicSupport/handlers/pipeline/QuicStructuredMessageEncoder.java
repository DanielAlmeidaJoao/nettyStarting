package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicStructuredMessageEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final String HANDLER_NAME="QuicMessageEncoder";
    public final TransmissionType type;

    private final QuicChannelMetrics metrics;

    public QuicStructuredMessageEncoder(QuicChannelMetrics metrics){
        this.metrics = metrics;
        type = TransmissionType.STRUCTURED_MESSAGE;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf message, ByteBuf byteBuf) {
        int bytes = message.readableBytes();
        message.markReaderIndex();
        message.readInt();
        byte msgType = message.readByte();
        message.resetReaderIndex();
        byteBuf.writeBytes(message);
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
            switch (msgType){
                case QUICLogics.APP_DATA:
                    q.setSentAppMessages(q.getSentAppMessages()+1);
                    q.setSentAppBytes(q.getSentAppBytes()+bytes+ QUICLogics.WRT_OFFSET);
                    break;
                case QUICLogics.KEEP_ALIVE:
                    q.setSentKeepAliveMessages(q.getSentKeepAliveMessages()+1);
                    break;
                case QUICLogics.HANDSHAKE_MESSAGE:
                    q.setSentControlMessages(q.getSentControlMessages()+1);
                    q.setSentControlBytes(q.getSentControlBytes()+bytes+ QUICLogics.WRT_OFFSET);
                    break;
                default:
                    throw new AssertionError("Unknown msg code in encoder: " + msgType);
            }
        }
    }

}
