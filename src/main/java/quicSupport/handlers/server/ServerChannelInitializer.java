package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final AtomicBoolean calledOnce;
    private final QuicListenerExecutor streamListenerExecutor;

    public ServerChannelInitializer(AtomicBoolean calledOnce, QuicListenerExecutor streamListenerExecutor) {
        this.calledOnce = calledOnce;
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        if(!calledOnce.get()){
            cp.addLast(new HandShakeHandler(streamListenerExecutor));
            calledOnce.set(true);
        }
        cp.addLast(new ServerStreamInboundHandler(streamListenerExecutor));
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
