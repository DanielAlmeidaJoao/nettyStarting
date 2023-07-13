package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Triggered when an established outbound connection is disconnected.
 */
public class OnConnectionDownEvent extends TCPEvent {

    public static final short EVENT_ID = 3;

    private final Host node;
    private final Throwable cause;
    public final String connectionId;
    public boolean inConnection;

    @Override
    public String toString() {
        return "OutConnectionDown{" +
                "node=" + node +
                ", cause=" + cause +
                ", connectionId= "+connectionId+
                ", inConnection="+inConnection+
                '}';
    }

    public OnConnectionDownEvent(Host node, Throwable cause, String streamId, boolean inConnection) {
        super(EVENT_ID);
        this.cause = cause;
        this.node = node;
        connectionId = streamId;
        this.inConnection = inConnection;
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
