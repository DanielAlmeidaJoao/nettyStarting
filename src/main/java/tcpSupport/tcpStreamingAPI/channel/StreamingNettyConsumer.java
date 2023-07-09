package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type);
    void onChannelMessageRead(String channelId, byte[] bytes);

    void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream);
    void onChannelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause);

    void onServerSocketBind(boolean success, Throwable cause);
    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

}
