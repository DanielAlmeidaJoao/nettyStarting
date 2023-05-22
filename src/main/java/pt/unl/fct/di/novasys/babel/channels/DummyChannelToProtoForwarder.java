package pt.unl.fct.di.novasys.babel.channels;

public class DummyChannelToProtoForwarder<T> implements ChannelListener<T>{
    @Override
    public void deliverMessage(T msg, Host from, String quicStreamId) {

    }

    @Override
    public void messageSent(T msg, Host to) {

    }

    @Override
    public void messageFailed(T msg, Host to, Throwable cause) {

    }

    @Override
    public void deliverEvent(ChannelEvent evt) {

    }
}
