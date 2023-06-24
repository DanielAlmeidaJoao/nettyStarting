package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;

/**
 * Triggered when an established outbound connection is disconnected.
 */
public class OutConnectionDown extends TCPEvent {

    public static final short EVENT_ID = 3;

    private final Host node;
    private final Throwable cause;
    public final String connectionId;

    @Override
    public String toString() {
        return "OutConnectionDown{" +
                "node=" + node +
                ", cause=" + cause +
                ", connectionId= "+connectionId+
                '}';
    }

    public OutConnectionDown(Host node, Throwable cause, String streamId) {
        super(EVENT_ID);
        this.cause = cause;
        this.node = node;
        connectionId = streamId;
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
