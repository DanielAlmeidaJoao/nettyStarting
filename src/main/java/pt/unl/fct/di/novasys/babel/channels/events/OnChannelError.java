package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.TransmissionType;

public class OnChannelError extends TCPEvent{

    public static final short EVENT_ID = 9;

    public final Host node;
    public final TransmissionType type;
    public final String conId;
    public final boolean inConnection;
    public final Throwable throwable;


    public OnChannelError(Host node, TransmissionType type, String conId, boolean inConnection, Throwable throwable) {
        super(EVENT_ID);
        this.node = node;
        this.type = type;
        this.conId = conId;
        this.inConnection = inConnection;
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        return "OnChannelError{" +
                "node=" + node +
                ", type=" + type +
                ", conId='" + conId + '\'' +
                ", inConnection=" + inConnection +
                ", throwable=" + throwable +
                '}';
    }
}
