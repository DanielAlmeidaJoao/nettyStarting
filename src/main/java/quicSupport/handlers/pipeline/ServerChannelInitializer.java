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
    private final boolean incoming;
    private final boolean defaultStream;
    private int cc = 0;
    public ServerChannelInitializer(QuicListenerExecutor streamListenerExecutor,
                                    QuicChannelMetrics quicChannelMetrics, boolean incoming,boolean defaultStream) {
        this.streamListenerExecutor = streamListenerExecutor;
        this.quicChannelMetrics = quicChannelMetrics;
        this.incoming = incoming;
        this.defaultStream = defaultStream;
        System.out.println("CHANNEL CREATED INITIALISED!!!");
        cc = 1;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        cc++;
        System.out.println("CHANNEL CREATED INITIALISED!!! INIT CALLED "+cc);
        ChannelPipeline cp = ch.pipeline();
        //cp.addLast(new LoggingHandler(LogLevel.INFO));
        if(quicChannelMetrics!=null){
            cp.addLast(new QuicMessageEncoder(quicChannelMetrics));
        }
        cp.addLast(new QuicDelimitedMessageDecoder(streamListenerExecutor,quicChannelMetrics,incoming));
        cp.addLast(new QuicStreamReadHandler(streamListenerExecutor,quicChannelMetrics,incoming));
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
