package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final QuicListenerExecutor streamListenerExecutor;

    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor, QuicChannelMetrics connectionMetrics) {
        this.streamListenerExecutor = streamListenerExecutor;
        System.out.println("CHANNEL CREATED INITIALISED!!!");
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        System.out.println("CHANNEL CREATED INITIALISED!!! INIT CALLED");
        ChannelPipeline cp = ch.pipeline();
        cp.addLast(new QuicDelimitedMessageDecoder(streamListenerExecutor));
        cp.addLast(new QuicStreamReadHandler(streamListenerExecutor));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CONNECTION ENDED!!!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
