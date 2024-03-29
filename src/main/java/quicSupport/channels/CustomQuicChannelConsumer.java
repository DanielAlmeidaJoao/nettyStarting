package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.net.InetSocketAddress;

public interface CustomQuicChannelConsumer {

    void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage controlData, InetSocketAddress remotePeer, TransmissionType type, int length, String customConId);
    void channelInactive(String channelId);

    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String id);

    void onKeepAliveMessage(String parentId, int i);

    void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, String customId, boolean inConnection, short sourceProto);

    void onReceivedDelimitedMessage(String streamId, BabelMessage babelMessage,int len);

    void onReceivedStream(String streamId, BabelOutputStream bytes);

    void streamInactiveHandler(QuicStreamChannel channel, String customId);

    void streamErrorHandler(QuicStreamChannel channel, Throwable throwable, String customId);

    String nextId();

    void onServerSocketBind(boolean success, Throwable cause);
    BabelMessageSerializer getSerializer();

}
