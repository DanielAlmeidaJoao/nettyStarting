package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Pair;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, Pair<InetSocketAddress, String> identification);
    void onChannelRead(Pair<InetSocketAddress, String> channelId, byte[] bytes, TransmissionType type);
    void onChannelInactive(Pair<InetSocketAddress, String> channelId);
    void onConnectionFailed(Pair<InetSocketAddress, String> channelId, Throwable cause);

    void onServerSocketBind(boolean success, Throwable cause);
    void handleOpenConnectionFailed(Pair<InetSocketAddress, String> peer, Throwable cause);

    String nextId();
}
