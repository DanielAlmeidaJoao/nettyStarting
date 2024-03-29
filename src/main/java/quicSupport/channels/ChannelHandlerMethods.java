package quicSupport.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface ChannelHandlerMethods{

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String conId);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(BabelMessage message,Throwable error, InetSocketAddress peer, TransmissionType type,String conID);
    void onStreamDataSent(InputStream inputStream, byte [] data, long len, Throwable error, InetSocketAddress peer, TransmissionType type, String conID);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection, TransmissionType type);

    void onChannelReadDelimitedMessage(String streamId, BabelMessage bytes, InetSocketAddress from);
    void onChannelReadFlowStream(String streamId, BabelOutputStream bytes, InetSocketAddress from, BabelInputStream inputStream, short streamProto);

    void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelInputStream babelInputStream);

    //void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
