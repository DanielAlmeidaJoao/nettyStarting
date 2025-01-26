package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.internal.MessageInEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageInEventClient;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 *
 */
@FunctionalInterface
public interface MessageInHandler<T extends ProtoMessage> {

    /**
     * Performs this operation on the ProtocolMessage.
     *
     * @param event the received message
     */
    void receive(MessageInEvent event, T msg);

}
