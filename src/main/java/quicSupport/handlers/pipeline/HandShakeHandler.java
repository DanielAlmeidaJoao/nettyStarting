package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;

//@ChannelHandler.Sharable
public class HandShakeHandler  extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(HandShakeHandler.class);
    public static final String NAME = "HandShakeHandler";
    private QuicListenerExecutor listener;
    public HandShakeHandler(QuicListenerExecutor listener){
        this.listener=listener;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("READING CONNECTION HANDSHAKE. ONLY ONCE/CONNECTION");
        ByteBuf buf = (ByteBuf) msg;
        byte [] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        HandShakeMessage shakeMessage = Logics.gson.fromJson(new String(bytes),HandShakeMessage.class);
        //listener.onChannelActive((QuicStreamChannel) ctx.channel(),shakeMessage,true);
        ctx.pipeline().remove(this);
        throw new Exception("NOT BEING USED!!!!");
    }
}