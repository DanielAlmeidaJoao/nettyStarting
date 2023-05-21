package quicSupport.channels;

import java.net.InetSocketAddress;

public interface ChannelHandlerMethods {

    void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    void failedToCloseStream(String streamId, Throwable reason);

    void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer);

    void failedToCreateStream(InetSocketAddress peer, Throwable error);

    void failedToGetMetrics(Throwable cause);

    void onStreamClosedHandler(InetSocketAddress peer, String streamId);

    void onStreamCreatedHandler(InetSocketAddress peer, String streamId);

    void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    void onConnectionUp(boolean incoming, InetSocketAddress peer);

    void onConnectionDown(InetSocketAddress peer, boolean incoming);

}
