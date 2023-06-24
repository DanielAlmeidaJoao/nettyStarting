package quicSupport.channels;

import org.apache.commons.lang3.tuple.Triple;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface ChannelHandlerMethods {

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(byte[] message, int len, Throwable error, InetSocketAddress peer, TransmissionType type);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection);

    void onStreamCreatedHandler(InetSocketAddress peer, String streamId, TransmissionType type, Triple<Short,Short,Short> triple);

    void onChannelReadDelimitedMessage(String streamId, byte[] bytes, InetSocketAddress from);
    void onChannelReadFlowStream(String streamId, byte[] bytes, InetSocketAddress from);

    void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId);

    //void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
