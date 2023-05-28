package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.ConnectionOrStreamType;

/**
 * Triggered when an incoming connection is established.
 */
public class InConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 2;

    private final Host node;
    public final ConnectionOrStreamType type;

    @Override
    public String toString() {
        return "InConnectionUp{" +
                "node=" + node +
                '}';
    }

    public InConnectionUp(Host node, ConnectionOrStreamType type) {
        super(EVENT_ID);
        this.node = node;
        this.type = type;
    }


    public Host getNode() {
        return node;
    }

}
