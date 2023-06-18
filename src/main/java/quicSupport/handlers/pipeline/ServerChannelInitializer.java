package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.metrics.QuicChannelMetrics;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics quicChannelMetrics;
    private final boolean incoming;
    private ConnectionId id;
    public ServerChannelInitializer(CustomQuicChannelConsumer consumer,
                                    QuicChannelMetrics quicChannelMetrics, boolean incoming, ConnectionId id) {
        this.consumer = consumer;
        this.quicChannelMetrics = quicChannelMetrics;
        this.incoming = incoming;
        this.id = id;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        //cp.addLast(new LoggingHandler(LogLevel.INFO));
        /**
        if(quicChannelMetrics!=null){
        } **/
        cp.addLast(QuicMessageEncoder.HANDLER_NAME,new QuicMessageEncoder(quicChannelMetrics));
        cp.addLast(QuicDelimitedMessageDecoder.HANDLER_NAME,new QuicDelimitedMessageDecoder(consumer,quicChannelMetrics,incoming,id));
        cp.addLast(QuicStreamHandler.HANDLER_NAME,new QuicStreamHandler(consumer,quicChannelMetrics,id));
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
