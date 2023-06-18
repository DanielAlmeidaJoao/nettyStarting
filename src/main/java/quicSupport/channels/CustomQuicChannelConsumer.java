package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage controlData, ConnectionId remotePeer, TransmissionType type);
    void channelInactive(ConnectionId channelId);

    void handleOpenConnectionFailed(ConnectionId peer, Throwable cause);

    void onKeepAliveMessage(ConnectionId parentId);

    void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, Triple<Short,Short,Short> triple, ConnectionId identification);

    void onReceivedDelimitedMessage(ConnectionId streamId, byte[] bytes);

    void onReceivedStream(Pair<InetSocketAddress, String> streamId, byte [] bytes);

    void streamClosedHandler(ConnectionId channel);

    void streamErrorHandler(ConnectionId channel, Throwable throwable);

    String nextId();

    }
