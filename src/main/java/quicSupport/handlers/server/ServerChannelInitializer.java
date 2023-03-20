package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final QuicListenerExecutor streamListenerExecutor;
    private AtomicBoolean allowHandshake;

    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor) {
        this.streamListenerExecutor = streamListenerExecutor;
        allowHandshake = new AtomicBoolean(true);
        System.out.println("CHANNEL CREATED INITIALISED!!!");
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        System.out.println("CHANNEL CREATED INITIALISED!!! INIT CALLED");
        ChannelPipeline cp = ch.pipeline();
        /**
        if(allowHandshake.get()){
            cp.addLast(new HandShakeHandler(streamListenerExecutor));
            allowHandshake.set(false);
        }**/
        //cp.addLast(new DelimitedMessageDecoder());
        cp.addLast(new QuicDelimitedMessageDecoder());
        cp.addLast(new ServerStreamInboundHandler(streamListenerExecutor));
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
