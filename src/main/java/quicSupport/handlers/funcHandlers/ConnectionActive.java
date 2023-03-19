package quicSupport.handlers.funcHandlers;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

@FunctionalInterface
public interface ConnectionActive {
    void execute(QuicStreamChannel defaultStream, HandShakeMessage serverAddress);
}
