package quicSupport.utils.entities;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class QUICConnectingOBJ {
    public final List<BabelMessage> msgWithLen;
    public final String conId;
    public final InetSocketAddress peer;
    public final short sourceProto, destProto;
    //public final String operation; //sendMessage or createStream
    public List<ConnectinOBJArgs> connectionsToOpen;

    public QUICConnectingOBJ(String conId, InetSocketAddress peer, short sourceProto, short destProto) {
        this.msgWithLen = new LinkedList<>();
        this.conId = conId;
        this.peer = peer;
        connectionsToOpen = null;
        this.sourceProto = sourceProto;
        this.destProto = destProto;
    }

    public void addToQueue(String customConId, TransmissionType type, short sourceProto, short destProto){
        if(connectionsToOpen == null){
            connectionsToOpen = new LinkedList<>();
        }
        connectionsToOpen.add(new ConnectinOBJArgs(customConId,type,sourceProto,destProto));
    }

}
