package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;

/**
 * Triggered when an incoming connection is established.
 */
public class InConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 2;

    private final Host node;
    public final TransmissionType type;
    public final String conId;

    @Override
    public String toString() {
        return "InConnectionUp{" +
                "node=" + node +
                '}';
    }

    public InConnectionUp(Host node, TransmissionType type, String customConId) {
        super(EVENT_ID);
        this.node = node;
        this.type = type;
        this.conId = customConId;
    }


    public Host getNode() {
        return node;
    }

}
