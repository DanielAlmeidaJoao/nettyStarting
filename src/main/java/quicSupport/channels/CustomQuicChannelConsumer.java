package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, byte [] controlData, InetSocketAddress remotePeer);
    void channelInactive(String channelId);

    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void onKeepAliveMessage(String parentId);

    void streamCreatedHandler(QuicStreamChannel channel);

    void streamReader(String streamId, byte[] bytes);

    void streamClosedHandler(QuicStreamChannel channel);

    void streamErrorHandler(QuicStreamChannel channel, Throwable throwable);

    }
