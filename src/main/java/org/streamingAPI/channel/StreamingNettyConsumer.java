package org.streamingAPI.channel;

import io.netty.channel.Channel;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage);
    void onChannelRead(String channelId, byte[] bytes);
    void onChannelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause);

    void onServerSocketBind(boolean success, Throwable cause);

}
