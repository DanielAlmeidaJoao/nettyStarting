package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.streamUtils.BabelInBytesWrapper;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type);
    void onChannelMessageRead(String channelId, byte[] bytes);

    void onChannelStreamRead(String channelId, BabelInBytesWrapper babelInBytesWrapper);
    void onChannelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause);

    void onServerSocketBind(boolean success, Throwable cause);
    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

}
