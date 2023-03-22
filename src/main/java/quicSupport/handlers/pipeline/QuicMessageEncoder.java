package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

public class QuicMessageEncoder extends MessageToByteEncoder {

    private final QuicChannelMetrics metrics;

    public QuicMessageEncoder(QuicChannelMetrics metrics){
        this.metrics = metrics;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf byteBuf) throws Exception {
        byteBuf.markReaderIndex();
        byteBuf.readInt();
        byte msgType = byteBuf.readByte();
        QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());

        switch (msgType){
            case Logics.APP_DATA:
                q.setSentAppMessages(q.getSentAppMessages()+1);
                q.setSentAppBytes(q.getSentAppBytes()+1);
                break;
            case Logics.HANDSHAKE_MESSAGE:
                q.setSentControlMessages(q.getSentControlMessages()+1);
                q.setSentControlBytes(q.getSentControlBytes()+1);
                break;
            default:
                throw new AssertionError("Unknown msg code in encoder: " + msgType);
        }
        byteBuf.resetReaderIndex();
    }
}
