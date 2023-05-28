package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetSocketAddress;

public interface TCPChannelHandlerMethods {
    void onChannelInactive(InetSocketAddress peer);
    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    void onMessageSent(byte[] data, InetSocketAddress peer, Throwable cause, ConnectionOrStreamType type);

    void onChannelActive(Channel channel, boolean handShakeMessage, InetSocketAddress peer, ConnectionOrStreamType type);

    void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from);
    void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from);
}
