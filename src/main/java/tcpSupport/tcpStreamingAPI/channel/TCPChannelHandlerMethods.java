package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface TCPChannelHandlerMethods {
    void onChannelInactive(InetSocketAddress peer, String conId);
    void onOpenConnectionFailed(InetSocketAddress peer, String conId, Throwable cause);
    void onMessageSent(byte[] data, InetSocketAddress peer, String conId, Throwable cause, TransmissionType type);

    void onChannelActive(String conId, boolean inConnection, InetSocketAddress peer, TransmissionType type);

    void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from);
    void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from);
}
