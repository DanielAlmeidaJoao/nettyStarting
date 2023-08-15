package udpSupport.metrics;

import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelStats {

    @Getter
    private Map<InetSocketAddress, UDPNetworkStatsWrapper> statsMap;


    public ChannelStats(boolean singleThreaded){
        if(singleThreaded){
            statsMap = new HashMap<>();
        }else {
            statsMap = new ConcurrentHashMap<>();
        }
    }

    public void addSentBytes(InetSocketAddress peer, long bytes, NetworkStatsKindEnum message_code){
        UDPNetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer, address -> new UDPNetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesSent(bytes);
    }
    public void addTransmissionRTT(InetSocketAddress peer, long timeElapsedMillis){
        UDPNetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer, address -> new UDPNetworkStatsWrapper(peer));
        networkStats.getStats(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED).addRTT(timeElapsedMillis);
    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,NetworkStatsKindEnum message_code){
        UDPNetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer, address -> new UDPNetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesReceived(bytes);
    }

    public List<UDPNetworkStatsWrapper> cloneChannelMetric(){
        List<UDPNetworkStatsWrapper> UDPNetworkStatsWrapperList = new LinkedList<>();
        for (UDPNetworkStatsWrapper value : statsMap.values()) {
            /**
            Map<NetworkStatsKindEnum,NetworkStats> statsMap = new HashMap<>(3);
            statsMap.put(NetworkStatsKindEnum.MESSAGE_STATS,clone(value.getStats(NetworkStatsKindEnum.MESSAGE_STATS)));
            statsMap.put(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED,clone(value.getStats(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED)));
            statsMap.put(NetworkStatsKindEnum.ACK_STATS,clone(value.getStats((NetworkStatsKindEnum.ACK_STATS))));
            **/
            UDPNetworkStatsWrapperList.add(new UDPNetworkStatsWrapper(value.getDest(),value.clone()));
        }
        return UDPNetworkStatsWrapperList;
    }
}
