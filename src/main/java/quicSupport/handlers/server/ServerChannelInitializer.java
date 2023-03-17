package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicClientExample;
import quicSupport.handlers.funcHandlers.StreamListenerExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private InNettyChannelListener listener;
    private final AtomicBoolean calledOnce;
    private final StreamListenerExecutor streamListenerExecutor;

    public ServerChannelInitializer(InNettyChannelListener listener, AtomicBoolean calledOnce, StreamListenerExecutor streamListenerExecutor) {
        this.listener = listener;
        this.calledOnce = calledOnce;
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        if(!calledOnce.get()){
            cp.addLast(new HandShakeHandler(listener));
            calledOnce.set(true);
        }
        cp.addLast(new ServerStreamInboundHandler(listener,streamListenerExecutor));
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
