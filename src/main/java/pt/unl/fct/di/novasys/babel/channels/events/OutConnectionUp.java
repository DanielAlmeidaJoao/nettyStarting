package pt.unl.fct.di.novasys.babel.channels.events;


import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;

/**
 * Triggered when a new outbound connection is established.
 */
public class OutConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 5;
    public TransmissionType type;
    private final Host node;

    @Override
    public String toString() {
        return "OutConnectionUp{" +
                "node=" + node +
                '}';
    }

    public OutConnectionUp(Host node, TransmissionType type) {
        super(EVENT_ID);
        this.node = node;
        this.type=type;
    }


    public Host getNode() {
        return node;
    }

}
