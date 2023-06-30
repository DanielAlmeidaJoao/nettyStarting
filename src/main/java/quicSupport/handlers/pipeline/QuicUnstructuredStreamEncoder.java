package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicUnstructuredStreamEncoder extends MessageToByteEncoder<ByteBuf> {
    public final TransmissionType type;
    public static final String HANDLER_NAME="QuicUnstructuredStreamEncoder";
    private final QuicChannelMetrics metrics;

    public QuicUnstructuredStreamEncoder(QuicChannelMetrics metrics){
        this.metrics = metrics;
        type = TransmissionType.UNSTRUCTURED_STREAM;
        System.out.println("OPENED UNSTRUCTERED IN "+HANDLER_NAME);
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf message, ByteBuf byteBuf){
        byteBuf.writeBytes(message);
        if(metrics!=null){
            int bytes = byteBuf.readableBytes();
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
            q.setSentAppMessages(q.getSentAppMessages()+1);
            q.setSentAppBytes(q.getSentAppBytes()+bytes);
        }
    }
}
