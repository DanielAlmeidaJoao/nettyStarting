package quicSupport.handlers.server;

import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        // Add a LineBasedFrameDecoder here as we just want to do some simple HTTP 0.9 handling.
        ch.pipeline().addLast(new LineBasedFrameDecoder(1024))
                .addLast(new ServerReaderInboundHandler());
    }
}
