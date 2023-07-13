package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;

public class DummyChannelToProtoForwarder<T> implements ChannelListener<T>{
    @Override
    public void deliverMessage(T msg, Host from, String quicStreamId) {

    }

    @Override
    public void messageSent(T msg, Host to, TransmissionType type) {

    }

    @Override
    public void messageFailed(T msg, Host to, Throwable cause, TransmissionType type) {

    }

    @Override
    public void deliverEvent(ChannelEvent evt) {

    }

    @Override
    public void deliverMessage(byte[] message, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId) {

    }

    @Override
    public void deliverStream(BabelOutputStream babelOutputStream, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId, BabelInputStream inputStream) {

    }

    @Override
    public int getChannelId() {
        return 0;
    }
}
