package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicServerExample;
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
        /** InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        System.out.println("ACTIVE "+address);
        HandShakeMessage handShakeMessage = new HandShakeMessage(address.getHostName(),address.getPort());
        listener.onChannelActive(channel,handShakeMessage); **/
        // Create streams etc..
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
