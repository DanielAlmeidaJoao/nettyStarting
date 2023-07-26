package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage controlData, InetSocketAddress remotePeer, TransmissionType type, int length);
    void channelInactive(String channelId);

    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String id);

    void onKeepAliveMessage(String parentId, int i);

    void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type,String customId, boolean inConnection);

    void onReceivedDelimitedMessage(String streamId, ByteBuf bytes);

    void onReceivedStream(String streamId, BabelOutputStream bytes);

    void streamInactiveHandler(QuicStreamChannel channel);

    void streamErrorHandler(QuicStreamChannel channel, Throwable throwable);

    String nextId();

    }
