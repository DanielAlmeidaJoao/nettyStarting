package quicSupport.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

public interface SendBytesInterface<T> {

    void send(String streamId, BabelMessage message);

}
