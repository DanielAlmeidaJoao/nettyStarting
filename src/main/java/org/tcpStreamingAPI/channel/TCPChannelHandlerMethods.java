package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface TCPChannelHandlerMethods {
    void onChannelInactive(InetSocketAddress peer);
    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    void onMessageSent(byte[] data, InetSocketAddress peer, Throwable cause, TransmissionType type);

    void onChannelActive(Channel channel, boolean handShakeMessage, InetSocketAddress peer, TransmissionType type);

    void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from);
    void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from);
}
