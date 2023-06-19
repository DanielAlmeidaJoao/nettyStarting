package tcpSupport.tcpStreamingAPI.handlerFunctions.receiver;

import io.netty.channel.Channel;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;

@FunctionalInterface
public interface StreamActiveHandler {
    public void execute(Channel channel, HandShakeMessage serverAddress);
}
