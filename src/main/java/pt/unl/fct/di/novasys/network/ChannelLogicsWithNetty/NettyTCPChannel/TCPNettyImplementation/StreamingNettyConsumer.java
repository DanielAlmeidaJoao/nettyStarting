package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.TCPNettyImplementation;

import io.netty.channel.Channel;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.connectionSetups.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.BabelOutputStream;

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
