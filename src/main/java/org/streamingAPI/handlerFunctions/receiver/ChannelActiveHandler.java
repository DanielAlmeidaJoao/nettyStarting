package org.streamingAPI.handlerFunctions.receiver;

import io.netty.channel.Channel;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;

@FunctionalInterface
public interface ChannelActiveHandler {
    public void execute(Channel channel, HandShakeMessage serverAddress);
}
