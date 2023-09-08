package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

import java.util.List;

public class QUICRawStreamDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(QUICRawStreamDecoder.class);

    public static final String HANDLER_NAME = "QUICRawStreamDecoder";
    private final CustomQuicChannelConsumer consumer;
    private final String customId;

    public QUICRawStreamDecoder(CustomQuicChannelConsumer streamListenerExecutor, boolean incoming, String customId){
        this.consumer=streamListenerExecutor;
        this.customId = customId;
    }

    /**
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        ByteBuf msg = (ByteBuf) in;
        int readAble = msg.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(msg.retainedDuplicate(),readAble);
        //msg.discardReadBytes();
        msg.release();
        consumer.onReceivedStream(customId,babelOutputStream);
    }
    **/



    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int available = in.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onReceivedStream(customId,babelOutputStream);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        consumer.streamErrorHandler((QuicStreamChannel) ctx.channel(),cause,customId);
        TCPChannelUtils.closeOnError(ctx.channel());
    }

}
