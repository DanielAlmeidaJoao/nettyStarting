package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface TCPChannelHandlerMethods {
    void onChannelInactive(InetSocketAddress peer);
    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    void onMessageSent(byte[] data, InetSocketAddress peer, Throwable cause, TransmissionType type);

    void onChannelActive(String channel, boolean handShakeMessage, InetSocketAddress peer, TransmissionType type);

    void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from, String conId);
    void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from, String conId);
}
