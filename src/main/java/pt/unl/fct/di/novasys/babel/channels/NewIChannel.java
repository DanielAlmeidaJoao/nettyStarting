package pt.unl.fct.di.novasys.babel.channels;

public interface NewIChannel<T> {
    void sendMessage(T var1, Host var2, short protoId);

    void closeConnection(Host var1, short protoId);

    void openConnection(Host var1, short protoId);

    void registerChannelInterest(short protoId);

    //exclusivelly for QUIC
    void sendMessage(String streamId, T msg, short proto);

    void createStream(Host peer);

    void closeStream(String streamId);
}
