package org.streamingAPI.handlerFunctions.receiver;

import io.netty.channel.Channel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface OpenConnectionFailedHandler {
    void execute(InetSocketAddress peer,Throwable cause);
}
