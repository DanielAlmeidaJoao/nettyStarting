package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;

import java.util.List;

public class QUICRawStreamDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(QUICRawStreamDecoder.class);

    public static final String HANDLER_NAME = "QUICRawStreamDecoder";
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QUICRawStreamDecoder(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming){
        this.consumer=streamListenerExecutor;
        this.metrics=metrics;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int readAble = msg.readableBytes();

        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
            q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
            q.setReceivedAppBytes(q.getReceivedAppBytes()+readAble+ QUICLogics.WRT_OFFSET);
        }

        BabelOutputStream babelOutputStream = new BabelOutputStream(msg.retainedSlice());
        msg.readerIndex(msg.readableBytes());
        consumer.onReceivedStream(ctx.channel().id().asShortText(), babelOutputStream);

    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
