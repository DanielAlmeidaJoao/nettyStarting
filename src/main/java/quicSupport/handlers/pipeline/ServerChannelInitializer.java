package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.entities.QuicChannelMetrics;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics quicChannelMetrics;

    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor,
                                    QuicChannelMetrics quicChannelMetrics) {
        this.streamListenerExecutor = streamListenerExecutor;
        this.quicChannelMetrics = quicChannelMetrics;
        System.out.println("CHANNEL CREATED INITIALISED!!!");
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        System.out.println("CHANNEL CREATED INITIALISED!!! INIT CALLED");
        ChannelPipeline cp = ch.pipeline();
        if(quicChannelMetrics!=null){
            cp.addLast(new QuicMessageEncoder(quicChannelMetrics));
        }
        cp.addLast(new QuicDelimitedMessageDecoder(streamListenerExecutor,quicChannelMetrics));
        cp.addLast(new QuicStreamReadHandler(streamListenerExecutor,quicChannelMetrics));
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
