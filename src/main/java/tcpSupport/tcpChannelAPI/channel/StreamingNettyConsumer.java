package tcpSupport.tcpChannelAPI.channel;

import io.netty.channel.Channel;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, int len);
    void onChannelMessageRead(String channelId, byte[] bytes);

    void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream);
    void onChannelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause, TransmissionType type);

    void onServerSocketBind(boolean success, Throwable cause);
    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType type, String conId);

}
