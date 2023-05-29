package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.commons.lang3.tuple.Triple;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, byte [] controlData, InetSocketAddress remotePeer, ConnectionOrStreamType type);
    void channelInactive(String channelId);

    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void onKeepAliveMessage(String parentId);

    void streamCreatedHandler(QuicStreamChannel channel, ConnectionOrStreamType type, Triple<Short,Short,Short> triple);

    void onReceivedDelimitedMessage(String streamId, byte[] bytes);

    void onReceivedStream(String streamId, byte [] bytes);

    void streamClosedHandler(QuicStreamChannel channel);

    void streamErrorHandler(QuicStreamChannel channel, Throwable throwable);

    }
