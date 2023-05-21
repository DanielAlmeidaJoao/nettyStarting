package pt.unl.fct.di.novasys.babel.channels;

public interface ChannelListener<T> {

    void deliverMessage(T msg, Host from, String quicStreamId);

    void messageSent(T msg, Host to);

    void messageFailed(T msg, Host to, Throwable cause);

    void deliverEvent(ChannelEvent evt);
}
