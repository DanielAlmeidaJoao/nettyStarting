package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.channels.CustomQuicChannelConsumer;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private final CustomQuicChannelConsumer consumer;
    private final boolean incoming;
    public ServerChannelInitializer(CustomQuicChannelConsumer consumer, boolean incoming) {
        this.consumer = consumer;
        this.incoming = incoming;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        ChannelPipeline cp = ch.pipeline();
        cp.addLast(QuicStructuredMessageEncoder.HANDLER_NAME,new QuicStructuredMessageEncoder());
        cp.addLast(QuicDelimitedMessageDecoder.HANDLER_NAME,new QuicDelimitedMessageDecoder(consumer,incoming));
        cp.addLast(QuicStreamReadHandler.HANDLER_NAME,new QuicStreamReadHandler(consumer));
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
