package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.BabelInputStream;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.BabelOutputStream;

public interface ChannelListener<T> {

    void deliverMessage(T msg, Host from, String quicStreamId);

    void messageSent(T msg, Host to, TransmissionType type);

    void messageFailed(T msg, Host to, Throwable cause, TransmissionType type);

    void deliverEvent(ChannelEvent evt);

    void deliverStream(BabelOutputStream babelOutputStream, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId, BabelInputStream inputStream);

    int getChannelId();
}
