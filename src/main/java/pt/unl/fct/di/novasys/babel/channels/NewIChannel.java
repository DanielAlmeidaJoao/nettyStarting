package pt.unl.fct.di.novasys.babel.channels;

public interface NewIChannel<T> {
    void sendMessage(T var1, Host var2, short protoId);

    void closeConnection(Host var1, short protoId);

    void openConnection(Host var1, short protoId);

    void registerChannelInterest(short protoId);
}
