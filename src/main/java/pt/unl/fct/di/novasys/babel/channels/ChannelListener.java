package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;

public interface ChannelListener<T> {

    void deliverMessage(T msg, Host from, String quicStreamId);

    void messageSent(T msg, Host to, TransmissionType type);

    void messageFailed(T msg, Host to, Throwable cause, TransmissionType type);

    void deliverEvent(ChannelEvent evt);

    void deliverMessage(byte [] message, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId);

    void deliverStream(BabelOutputStream babelOutputStream, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId, BabelInputStream inputStream);

    int getChannelId();
}
