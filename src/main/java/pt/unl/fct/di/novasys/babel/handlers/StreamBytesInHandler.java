package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.internal.BabelInBytesWrapperEvent;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 *
 */
@FunctionalInterface
public interface StreamBytesInHandler {

    /**
     * Performs this operation on the ProtocolMessage.
     *
     * @param msg the received message
     */
    void receive(BabelInBytesWrapperEvent dataInEvent);

}
