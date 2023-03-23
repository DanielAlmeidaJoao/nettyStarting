package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface SocketBindHandler {
    void execute(boolean success, Throwable cause);
}
