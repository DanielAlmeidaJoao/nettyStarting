package tcpSupport.tcpStreamingAPI.handlerFunctions.receiver;

import io.netty.channel.Channel;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;

@FunctionalInterface
public interface ChannelActiveHandler {
    public void execute(Channel channel, HandShakeMessage serverAddress);
}
