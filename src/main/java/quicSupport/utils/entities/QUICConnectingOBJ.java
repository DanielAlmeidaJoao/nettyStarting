package quicSupport.utils.entities;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import quicSupport.utils.enums.StreamType;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class QUICConnectingOBJ {
    public final List<Pair<byte [],Integer>> msgWithLen;
    public final String conId;
    public final InetSocketAddress peer;
    //public final String operation; //sendMessage or createStream
    public List<Triple<String,TransmissionType, StreamType>> connectionsToOpen;

    public QUICConnectingOBJ(String conId, InetSocketAddress peer) {
        this.msgWithLen = new LinkedList<>();
        this.conId = conId;
        this.peer = peer;
        connectionsToOpen = null;
    }

    public void addToQueue(String customConId, TransmissionType type, StreamType streamType){
        if(connectionsToOpen == null){
            connectionsToOpen = new LinkedList<>();
        }
        connectionsToOpen.add(Triple.of(customConId,type,streamType));
    }

}
