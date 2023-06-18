package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public final Pair<InetSocketAddress,String> id;
    private Queue<String> otherConnectionsToSamePeer;
    public CustomTCPConnection(Channel channel, TransmissionType type,Pair<InetSocketAddress,String> id){
        this.channel=channel;
        this.type=type;
        this.id = id;
    }
    public void addOtherConnections(String id){
        if(otherConnectionsToSamePeer==null){
            otherConnectionsToSamePeer = new LinkedList<>();
        }
        otherConnectionsToSamePeer.add(id);
    }
    public void removeOtherConnections(String id){
        if(otherConnectionsToSamePeer!=null){
            otherConnectionsToSamePeer.remove(id);
        }
    }
    public String getAndRemove(){
        if(otherConnectionsToSamePeer==null){
            return null;
        }else{
            return otherConnectionsToSamePeer.remove();
        }
    }
    public void setQueue(Queue<String> other){
        assert otherConnectionsToSamePeer == null;
        otherConnectionsToSamePeer = other;
    }
    public Queue<String> getQueue(){
        assert otherConnectionsToSamePeer != null;
        return otherConnectionsToSamePeer;
    }
}
