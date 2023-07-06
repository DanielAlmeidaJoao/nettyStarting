package quicSupport.utils.entities;

import org.apache.commons.lang3.tuple.Pair;
import quicSupport.utils.enums.TransmissionType;
//hello
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class QUICConnectingOBJ {
    public final List<Pair<byte [],Integer>> msgWithLen;
    public final String conId;
    public final InetSocketAddress peer;
    //public final String operation; //sendMessage or createStream
    public List<Pair<String,TransmissionType>> connectionsToOpen;

    public QUICConnectingOBJ(String conId, InetSocketAddress peer) {
        this.msgWithLen = new LinkedList<>();
        this.conId = conId;
        this.peer = peer;
        connectionsToOpen = null;
    }

    public void addToQueue(String customConId, TransmissionType type){
        if(connectionsToOpen == null){
            connectionsToOpen = new LinkedList<>();
        }
        connectionsToOpen.add(Pair.of(customConId,type));
    }

}
