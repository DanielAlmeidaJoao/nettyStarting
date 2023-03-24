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
        if(metrics==null){
            return;
        }
        ByteBuf data = (ByteBuf) o;
        byteBuf.writeBytes(data);
        byteBuf.markReaderIndex();
        int bytes = byteBuf.readInt();
        byte msgType = byteBuf.readByte();
        QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());

        switch (msgType){
            case Logics.APP_DATA:
                q.setSentAppMessages(q.getSentAppMessages()+1);
                q.setSentAppBytes(q.getSentAppBytes()+bytes+Logics.WRT_OFFSET);
                break;
            case Logics.KEEP_ALIVE:
                q.setSentKeepAliveMessages(q.getSentKeepAliveMessages()+1);
            break;
            case Logics.HANDSHAKE_MESSAGE:
                q.setSentControlMessages(q.getSentControlMessages()+1);
                q.setSentControlBytes(q.getSentControlBytes()+bytes+Logics.WRT_OFFSET);
                break;
            default:
                throw new AssertionError("Unknown msg code in encoder: " + msgType);
        }
        byteBuf.resetReaderIndex();
    }
}
