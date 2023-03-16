package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.client_server.QuicClientExample;
import quicSupport.utils.Logic;

import java.util.concurrent.atomic.AtomicBoolean;

@ChannelHandler.Sharable
public class HandShakeHandler  extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(HandShakeHandler.class);
    public static final String NAME = "HandShakeHandler";
    private InNettyChannelListener listener;
    public HandShakeHandler(InNettyChannelListener listener){
        this.listener=listener;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("READING CONNECTION HANDSHAKE. ONLY ONCE/CONNECTION");
        ByteBuf buf = (ByteBuf) msg;
        byte [] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        HandShakeMessage shakeMessage = Logic.gson.fromJson(new String(bytes),HandShakeMessage.class);
        listener.onChannelActive(ctx.channel().parent(),shakeMessage);
        System.out.println(buf.toString(CharsetUtil.US_ASCII));
        ctx.pipeline().remove(this);
    }
}
