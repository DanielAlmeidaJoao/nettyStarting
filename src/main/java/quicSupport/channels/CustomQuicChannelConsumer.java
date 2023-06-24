package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, byte [] controlData, InetSocketAddress remotePeer, TransmissionType type);
    void channelInactive(String channelId);

    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void onKeepAliveMessage(String parentId);

    void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type,String customId, boolean inConnection);

    void onReceivedDelimitedMessage(String streamId, byte[] bytes);

    void onReceivedStream(String streamId, byte [] bytes);

    void streamInactiveHandler(QuicStreamChannel channel);

    void streamErrorHandler(QuicStreamChannel channel, Throwable throwable);

    String nextId();

    }
