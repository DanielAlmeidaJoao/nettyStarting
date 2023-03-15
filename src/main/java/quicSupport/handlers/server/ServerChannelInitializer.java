package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

public class ServerChannelInitializer extends ChannelInitializer<QuicStreamChannel> {
    private InNettyChannelListener listener;

    public ServerChannelInitializer(InNettyChannelListener listener) {
        this.listener = listener;
    }

    @Override
    protected void initChannel(QuicStreamChannel ch)  {
        System.out.println("STREAM CHANNEL ACTIVE "+ch.id().asShortText());
        // Add a LineBasedFrameDecoder here as we just want to do some simple HTTP 0.9 handling.
        ch.pipeline()//.addLast(new LineBasedFrameDecoder(1024))
                .addLast(new ServerReaderInboundHandler(listener));

    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
