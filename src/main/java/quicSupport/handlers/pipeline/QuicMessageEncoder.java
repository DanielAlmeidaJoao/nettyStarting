package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.entities.MessageToByteEncoderParameter;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicMessageEncoder extends MessageToByteEncoder<MessageToByteEncoderParameter> {

    private final QuicChannelMetrics metrics;

    public QuicMessageEncoder(QuicChannelMetrics metrics){
        this.metrics = metrics;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, MessageToByteEncoderParameter message, ByteBuf byteBuf) throws Exception {
        /**
        if(metrics==null){
            return;
        }**/
        byteBuf.writeInt(message.getDataLen());
        byteBuf.writeByte(message.getMsgCode());
        byteBuf.writeBytes(message.getData(),0, message.getDataLen());
        //byteBuf.markReaderIndex();

        int bytes = message.getDataLen();// byteBuf.readInt(); //TODO MAKES NO SENSE ...
        byte msgType = message.getMsgCode(); // byteBuf.readByte();
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
        //byteBuf.resetReaderIndex();
    }
}
