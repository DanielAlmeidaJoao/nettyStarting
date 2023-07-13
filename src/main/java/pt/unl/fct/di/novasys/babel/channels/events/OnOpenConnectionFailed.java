package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.TransmissionType;

/**
 * Triggered when an outbound connection fails to establish.
 */
public class OnOpenConnectionFailed<T>  extends TCPEvent {

    public static final short EVENT_ID = 5;
    public final Host node;
    private final Throwable cause;
    public final String connectionId;
    public final TransmissionType type;
    @Override
    public String toString() {
        return "OnOpenConnectionFailed{" +
                "node=" + node +
                ", cause=" + cause +
                '}';
    }

    public OnOpenConnectionFailed(Host node, String connectionId, TransmissionType type, Throwable cause) {
        super(EVENT_ID);
        this.cause = cause;
        this.node = node;
        this.connectionId = connectionId;
        this.type = type;
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
