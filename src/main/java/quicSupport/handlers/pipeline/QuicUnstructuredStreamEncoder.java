package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.entities.MessageToByteEncoderParameter;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicUnstructuredStreamEncoder extends MessageToByteEncoder<MessageToByteEncoderParameter> {
    public final ConnectionOrStreamType type;
    public static final String HANDLER_NAME="QuicUnstructuredStreamEncoder";
    private final QuicChannelMetrics metrics;

    public QuicUnstructuredStreamEncoder(QuicChannelMetrics metrics){
        this.metrics = metrics;
        type = ConnectionOrStreamType.UNSTRUCTURED_STREAM;
        System.out.println("OPENED UNSTRUCTERED IN "+HANDLER_NAME);
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, MessageToByteEncoderParameter message, ByteBuf byteBuf){
        if(type!=message.connectionOrStreamType){
            throw new RuntimeException("SENDING MESSAGE DATA TO A STREAM DATA CONNECTION");
        }
        byteBuf.writeBytes(message.getData(),0, message.getDataLen());
        int bytes = message.getDataLen();
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
            q.setSentAppMessages(q.getSentAppMessages()+1);
            q.setSentAppBytes(q.getSentAppBytes()+bytes+ QUICLogics.WRT_OFFSET);
        }
    }
}
