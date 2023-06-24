package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

public class QUICRawStreamDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(QUICRawStreamDecoder.class);

    public static final String HANDLER_NAME = "QUICRawStreamDecoder";
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QUICRawStreamDecoder(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming){
        this.consumer=streamListenerExecutor;
        this.metrics=metrics;
        System.out.println("REGISTERED REGISTERED GG");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        byte [] data = new byte[msg.readableBytes()];
        System.out.println("RECEIVED DATA TATA "+data.length);

        msg.readBytes(data);
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        consumer.onReceivedStream(ch.id().asShortText(),data);
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
            q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
            q.setReceivedAppBytes(q.getReceivedAppBytes()+data.length+ QUICLogics.WRT_OFFSET);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
