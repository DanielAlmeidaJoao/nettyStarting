package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

public class QUICRawStreamDecoder extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QUICRawStreamDecoder.class);

    public static final String HANDLER_NAME = "QUICRawStreamDecoder";
    private final CustomQuicChannelConsumer consumer;

    public QUICRawStreamDecoder(CustomQuicChannelConsumer streamListenerExecutor, boolean incoming){
        this.consumer=streamListenerExecutor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        ByteBuf msg = (ByteBuf) in;
        int readAble = msg.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(msg.duplicate(),readAble);
        msg.readerIndex(readAble);
        consumer.onReceivedStream(ctx.channel().id().asShortText(), babelOutputStream);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
