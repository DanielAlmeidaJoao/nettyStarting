package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.channels.Host;

/**
 * Triggered when an incoming connection is established.
 */
public class InConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 2;

    private final Host node;

    @Override
    public String toString() {
        return "InConnectionUp{" +
                "node=" + node +
                '}';
    }

    public InConnectionUp(Host node) {
        super(EVENT_ID);
        this.node = node;
    }


    public Host getNode() {
        return node;
    }

}
