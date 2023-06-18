package quicSupport.channels;

import org.apache.commons.lang3.tuple.Triple;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface ChannelHandlerMethods {

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(byte[] message, int len, Throwable error, InetSocketAddress peer, TransmissionType type);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId);

    void onStreamCreatedHandler(ConnectionId identification, TransmissionType type, Triple<Short,Short,Short> triple);

    void onChannelReadDelimitedMessage(ConnectionId connectionId, byte[] bytes);
    void onChannelReadFlowStream(ConnectionId connectionId, byte[] bytes);

    void onConnectionUp(boolean incoming,ConnectionId connectionId ,TransmissionType type);

    void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
