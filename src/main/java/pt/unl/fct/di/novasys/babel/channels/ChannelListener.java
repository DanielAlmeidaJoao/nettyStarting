package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.ConnectionOrStreamType;

public interface ChannelListener<T> {

    void deliverMessage(T msg, Host from, String quicStreamId);

    void messageSent(T msg, Host to, ConnectionOrStreamType type);

    void messageFailed(T msg, Host to, Throwable cause, ConnectionOrStreamType type);

    void deliverEvent(ChannelEvent evt);

    void deliverMessage(byte [] message, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId);
    int getChannelId();
}
