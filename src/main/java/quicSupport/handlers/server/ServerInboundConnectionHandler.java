package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.client_server.QuicServerExample;

public class ServerInboundConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        QuicChannel channel = (QuicChannel) ctx.channel();
        System.out.println("ACTIVE "+ctx.channel().remoteAddress());
        // Create streams etc..
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        ((QuicChannel) ctx.channel()).collectStats().addListener(f -> {
            if (f.isSuccess()) {
                LOGGER.info("Connection closed: {}", f.getNow());
            }
        });
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
