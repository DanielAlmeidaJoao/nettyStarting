package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final QuicListenerExecutor streamListenerExecutor;

    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor) {
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        cp.addLast(new HandShakeHandler(streamListenerExecutor));
        cp.addLast(new DelimitedMessageDecoder());
        cp.addLast(new ServerStreamInboundHandler(streamListenerExecutor));
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
