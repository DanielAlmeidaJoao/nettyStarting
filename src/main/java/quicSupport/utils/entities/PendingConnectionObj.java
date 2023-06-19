package quicSupport.utils.entities;

import org.apache.commons.lang3.tuple.Pair;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class PendingConnectionObj {
    public final List<Pair<byte [],Integer>> msgWithLen;
    public final String conId;
    public final InetSocketAddress peer;
    public final String operation; //sendMessage or createStream

    public PendingConnectionObj(String conId, InetSocketAddress peer, String operation) {
        this.msgWithLen = new LinkedList<>();
        this.conId = conId;
        this.peer = peer;
        this.operation = operation;
    }
}
