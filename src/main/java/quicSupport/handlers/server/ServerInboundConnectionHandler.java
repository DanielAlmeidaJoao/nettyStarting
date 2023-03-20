package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.client.QuicStreamReadHandler;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

public class ServerInboundConnectionHandler extends ChannelInboundHandlerAdapter {

    private QuicListenerExecutor listener;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    public ServerInboundConnectionHandler(QuicListenerExecutor listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("CHANNEL ACTIVE");
        QuicChannel cd = (QuicChannel) ctx.channel();
        System.out.println(cd.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL));
        /** InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        System.out.println("ACTIVE "+address);
        HandShakeMessage handShakeMessage = new HandShakeMessage(address.getHostName(),address.getPort());
        listener.onChannelActive(channel,handShakeMessage); **/
        // Create streams etc..
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CHANNEL INACTIVE SERVER");
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("SOMETHING HAPPEPNED "+evt);
    }

/*    @Override
    public boolean isSharable() {
        return true;
    }*/
}
