package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.handlers.nettyFuncHandlers.QuicListenerExecutor;
import quicSupport.utils.metrics.QuicChannelMetrics;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics quicChannelMetrics;
    private final boolean incoming;
    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor,
                                    QuicChannelMetrics quicChannelMetrics, boolean incoming) {
        this.streamListenerExecutor = streamListenerExecutor;
        this.quicChannelMetrics = quicChannelMetrics;
        this.incoming = incoming;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        //cp.addLast(new LoggingHandler(LogLevel.INFO));
        if(quicChannelMetrics!=null){
            cp.addLast(new QuicMessageEncoder(quicChannelMetrics));
        }
        cp.addLast(new QuicDelimitedMessageDecoder(streamListenerExecutor,quicChannelMetrics,incoming));
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
