package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.connectionSetups;

import io.netty.channel.EventLoopGroup;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface ClientInterface {

    void connect(InetSocketAddress peer, TransmissionType type, String conId, short destProto) throws Exception;
    void shutDown();

    EventLoopGroup getEventLoopGroup();

}
