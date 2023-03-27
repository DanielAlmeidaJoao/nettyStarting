package org.streamingAPI.channel;

import io.netty.channel.Channel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

public interface StreamingNettyConsumer {

    void channelActive(Channel channel, HandShakeMessage handShakeMessage);
    void channelRead(String channelId, byte[] bytes);
    void channelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause);

}
