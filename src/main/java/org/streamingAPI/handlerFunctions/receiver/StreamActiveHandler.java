package org.streamingAPI.handlerFunctions.receiver;

import io.netty.channel.Channel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

@FunctionalInterface
public interface StreamActiveHandler {
    public void execute(Channel channel, HandShakeMessage serverAddress);
}
