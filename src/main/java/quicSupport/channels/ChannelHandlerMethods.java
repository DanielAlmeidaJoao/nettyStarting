package quicSupport.channels;

import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface ChannelHandlerMethods<T> {

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String conId);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(T message,Throwable error, InetSocketAddress peer, TransmissionType type,String conID);
    void onStreamDataSent(InputStream inputStream,byte [] data, int len, Throwable error, InetSocketAddress peer, TransmissionType type,String conID);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection);

    void onChannelReadDelimitedMessage(String streamId, T bytes, InetSocketAddress from);
    void onChannelReadFlowStream(String streamId, BabelOutputStream bytes, InetSocketAddress from, BabelInputStream inputStream);

    void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelInputStream babelInputStream);

    //void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
