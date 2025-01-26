package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.connectionSetups;

import io.netty.channel.EventLoopGroup;

public interface ServerInterface {

    void startServer() throws Exception;
    void shutDown();

    EventLoopGroup getEventLoopGroup();

}
