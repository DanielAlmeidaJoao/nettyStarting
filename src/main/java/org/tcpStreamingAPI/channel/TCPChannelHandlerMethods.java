package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;

import java.net.InetSocketAddress;

public interface TCPChannelHandlerMethods {
    void onChannelInactive(InetSocketAddress peer);
    void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    void sendFailed(InetSocketAddress peer, Throwable reason);
    void sendSuccess(byte[] data, InetSocketAddress peer);

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, InetSocketAddress peer);

    void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);
}
