package pt.unl.fct.di.novasys.babel.channels.events;

import lombok.NonNull;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;

/**
 * Triggered when an incoming connection is established.
 */
public class OnStreamConnectionUpEvent extends TCPEvent {

    public static final short EVENT_ID = 2;

    private final Host node;
    public final TransmissionType type;
    public final String conId;
    public final boolean inConnection;
    public final BabelInputStream babelInputStream;

    @Override
    public String toString() {
        return "OnStreamConnectionUpEvent{" +
                "node=" + node +
                "type="+type+
                "conId="+conId+
                "inConnection="+inConnection+
                '}';
    }

    public OnStreamConnectionUpEvent(Host node, String customConId, boolean inConnection, @NonNull BabelInputStream babelInputStream) {
        super(EVENT_ID);
        this.node = node;
        this.type = TransmissionType.UNSTRUCTURED_STREAM;
        this.conId = customConId;
        this.inConnection = inConnection;
        this.babelInputStream = babelInputStream;
    }


    public Host getNode() {
        return node;
    }

}
