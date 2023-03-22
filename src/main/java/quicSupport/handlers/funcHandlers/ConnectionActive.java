package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.entities.ControlDataEntity;

@FunctionalInterface
public interface ConnectionActive {
    void execute(QuicStreamChannel defaultStream, ControlDataEntity controlEntity, long sentOrReceiveBytes, boolean incomming);
}
