package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.streamUtils.BabelInBytesWrapper;

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
    public void deliverMessage(BabelInBytesWrapper babelInBytesWrapper, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId) {

    }

    @Override
    public int getChannelId() {
        return 0;
    }
}
