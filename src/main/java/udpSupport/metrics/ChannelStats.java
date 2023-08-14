package udpSupport.metrics;

import lombok.Getter;
import org.modelmapper.ModelMapper;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelStats {

    @Getter
    private Map<InetSocketAddress,NetworkStatsWrapper> statsMap;

    public final ModelMapper modelMapper;

    public ChannelStats(boolean singleThreaded){
        if(singleThreaded){
            statsMap = new HashMap<>();
        }else {
            statsMap = new ConcurrentHashMap<>();
        }
        modelMapper = new ModelMapper();
    }

    public void addSentBytes(InetSocketAddress peer, long bytes, NetworkStatsKindEnum message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesSent(bytes);
    }
    public void addTransmissionRTT(InetSocketAddress peer, long timeElapsedMillis){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        networkStats.getStats(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED).addRTT(timeElapsedMillis);
    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,NetworkStatsKindEnum message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesReceived(bytes);
    }
    private NetworkStatsWrapper clone(NetworkStatsWrapper stats){
        return modelMapper.map(stats,NetworkStatsWrapper.class);
    }
    public List<NetworkStatsWrapper> cloneChannelMetric(){
        List<NetworkStatsWrapper> networkStatsWrapperList = new LinkedList<>();
        for (NetworkStatsWrapper value : statsMap.values()) {
            /**
            Map<NetworkStatsKindEnum,NetworkStats> statsMap = new HashMap<>(3);
            statsMap.put(NetworkStatsKindEnum.MESSAGE_STATS,clone(value.getStats(NetworkStatsKindEnum.MESSAGE_STATS)));
            statsMap.put(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED,clone(value.getStats(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED)));
            statsMap.put(NetworkStatsKindEnum.ACK_STATS,clone(value.getStats((NetworkStatsKindEnum.ACK_STATS))));
            **/
            networkStatsWrapperList.add(new NetworkStatsWrapper(value.getDest(),clone(value)));
        }
        return networkStatsWrapperList;
    }
}
