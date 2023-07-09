package quicSupport.channels;

import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.streamUtils.BabelInBytesWrapper;
import tcpSupport.tcpStreamingAPI.utils.BabelStream;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface ChannelHandlerMethods {

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(byte[] message, InputStream inputStream, int len, Throwable error, InetSocketAddress peer, TransmissionType type);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection);

    void onChannelReadDelimitedMessage(String streamId, byte[] bytes, InetSocketAddress from);
    void onChannelReadFlowStream(String streamId, BabelInBytesWrapper bytes, InetSocketAddress from);

    void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelStream babelStream);

    //void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
