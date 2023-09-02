package tcpSupport.tcpChannelAPI.channel;

import io.netty.channel.Channel;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.net.InetSocketAddress;

public interface StreamingNettyConsumer {

    void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, int len);
    void onChannelMessageRead(String channelId, BabelMessage babelMessage, int bytes);

    void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream);
    void onChannelInactive(String channelId);
    void onConnectionFailed(String channelId, Throwable cause, TransmissionType type);

    void onServerSocketBind(boolean success, Throwable cause);
    void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType type, String conId);

    void channelError(InetSocketAddress address, Throwable throwable, String nettyID);

    BabelMessageSerializer getSerializer();

}
