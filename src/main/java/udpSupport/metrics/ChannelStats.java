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

    public void addSentBytes(InetSocketAddress peer, long bytes){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper());
        networkStats.getMessageStats().addBytesSent(bytes);

    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,byte message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper());
        switch (message_code){
            case UDPLogics.APP_MESSAGE: networkStats.getMessageStats().addBytesReceived(bytes);
                                        networkStats.getAckStats().addBytesSent(1); break;
            case UDPLogics.APP_ACK: networkStats.getAckStats().addBytesReceived(bytes);break;
        }
    }
}
