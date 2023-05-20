package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;

/**
 * Triggered when an incoming connection is disconnected.
 */
public class InConnectionDown extends TCPEvent {

    public static final short EVENT_ID = 1;

    private final Host node;
    private final Throwable cause;

    @Override
    public String toString() {
        return "InConnectionDown{" +
                "node=" + node +
                ", cause=" + cause +
                '}';
    }

    public InConnectionDown(Host node, Throwable cause) {
        super(EVENT_ID);
        this.cause = cause;
        this.node = node;
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
