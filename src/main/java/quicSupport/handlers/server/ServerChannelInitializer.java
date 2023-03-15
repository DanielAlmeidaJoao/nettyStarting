package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        System.out.println("STREAM CHANNEL ACTIVE "+ch.id().asShortText());
        // Add a LineBasedFrameDecoder here as we just want to do some simple HTTP 0.9 handling.
        ch.pipeline()//.addLast(new LineBasedFrameDecoder(1024))
                .addLast(new ServerReaderInboundHandler());

    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
