package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.QuicNettyImplementations;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

public interface SendBytesInterface<T> {

    void send(String streamId, BabelMessage message);

}
