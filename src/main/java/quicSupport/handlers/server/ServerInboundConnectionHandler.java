package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.client_server.QuicServerExample;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ServerInboundConnectionHandler extends ChannelInboundHandlerAdapter {

    private InNettyChannelListener listener;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    public ServerInboundConnectionHandler(InNettyChannelListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("++++");
        System.out.println(ctx.name());
        QuicChannel channel = (QuicChannel) ctx.channel();
        System.out.println(ctx.channel().remoteAddress());
        System.out.println(channel.id().asLongText());
        System.out.println("++++");
        /** InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        System.out.println("ACTIVE "+address);
        HandShakeMessage handShakeMessage = new HandShakeMessage(address.getHostName(),address.getPort());
        listener.onChannelActive(channel,handShakeMessage); **/
        // Create streams etc..
    }
    public void channelInactive(ChannelHandlerContext ctx) {
        ((QuicChannel) ctx.channel()).collectStats().addListener(f -> {
            if (f.isSuccess()) {
                LOGGER.info("Connection closed: {}", f.getNow());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
    @Override
    public boolean isSharable() {
        return true;
    }
}
