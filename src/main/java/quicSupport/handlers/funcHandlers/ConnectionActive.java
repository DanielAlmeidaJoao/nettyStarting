package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.entities.ControlDataEntity;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ConnectionActive {
    void execute(QuicStreamChannel channel, byte [] controlData,InetSocketAddress remotePeer);
}
