package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;

/**
 * Triggered when an incoming connection is established.
 */
public class OnMessageConnectionUpEvent extends TCPEvent {

    public static final short EVENT_ID = 4;

    private final Host node;
    public final TransmissionType type;
    public final String conId;
    public final boolean inConnection;

    @Override
    public String toString() {
        return "OnStreamConnectionUpEvent{" +
                "node=" + node +
                "type="+type+
                "conId="+conId+
                "inConnection="+inConnection+
                '}';
    }

    public OnMessageConnectionUpEvent(Host node,String customConId, boolean inConnection) {
        super(EVENT_ID);
        this.node = node;
        this.type = TransmissionType.STRUCTURED_MESSAGE;
        this.conId = customConId;
        this.inConnection = inConnection;
    }


    public Host getNode() {
        return node;
    }

}
