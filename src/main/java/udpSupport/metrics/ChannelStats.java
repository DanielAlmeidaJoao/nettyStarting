package udpSupport.metrics;

import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ChannelStats {

    private Map<InetSocketAddress,NetworkStatsWrapper> statsMap;
    public ChannelStats(){
        statsMap = new HashMap<>();
    }

    public void addSentBytes(InetSocketAddress peer, long bytes, byte message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper());
        switch (message_code){
            case UDPLogics.APP_MESSAGE: networkStats.getMessageStats().addBytesSent(bytes);break;
            case UDPLogics.APP_ACK: networkStats.getAckStats().addBytesSent(bytes);break;
        }
    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,byte message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper());
        switch (message_code){
            case UDPLogics.APP_MESSAGE: networkStats.getMessageStats().addBytesReceived(bytes);break;
            case UDPLogics.APP_ACK: networkStats.getAckStats().addBytesReceived(bytes);break;
        }
    }
}
