package pt.unl.fct.di.novasys.babel.channels.events;


import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.ConnectionOrStreamType;

/**
 * Triggered when a new outbound connection is established.
 */
public class OutConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 5;
    public ConnectionOrStreamType type;
    private final Host node;

    @Override
    public String toString() {
        return "OutConnectionUp{" +
                "node=" + node +
                '}';
    }

    public OutConnectionUp(Host node, ConnectionOrStreamType type) {
        super(EVENT_ID);
        this.node = node;
        this.type=type;
    }


    public Host getNode() {
        return node;
    }

}
