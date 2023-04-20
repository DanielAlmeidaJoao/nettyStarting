package udpSupport.metrics;

import lombok.Getter;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ChannelStats {

    @Getter
    private Map<InetSocketAddress,NetworkStatsWrapper> statsMap;
    public ChannelStats(){
        statsMap = new HashMap<>();
    }
    public ChannelStats(Map<InetSocketAddress,NetworkStatsWrapper> map){
        statsMap = map;
    }

    public void addSentBytes(InetSocketAddress peer, long bytes, NetworkStatsKindEnum message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesSent(bytes);
    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,NetworkStatsKindEnum message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        networkStats.getStats(message_code).addBytesReceived(bytes);
    }
    private NetworkStats clone(NetworkStats stats){
        return UDPLogics.modelMapper.map(stats,NetworkStats.class);
    }
    public ChannelStats cloneChannelMetric(){
        Map<InetSocketAddress,NetworkStatsWrapper> stats = new HashMap<>(statsMap.size());
        for (NetworkStatsWrapper value : statsMap.values()) {
            Map<NetworkStatsKindEnum,NetworkStats> statsMap = new HashMap<>(3);
            statsMap.put(NetworkStatsKindEnum.MESSAGE_STATS,clone(value.getStats(NetworkStatsKindEnum.MESSAGE_STATS)));
            statsMap.put(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED,clone(value.getStats(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED)));
            statsMap.put(NetworkStatsKindEnum.ACK_STATS,clone(value.getStats((NetworkStatsKindEnum.ACK_STATS))));
            NetworkStatsWrapper copy = new NetworkStatsWrapper(value.getDest(),statsMap);
            stats.put(value.getDest(),copy);
        }
        return new ChannelStats(stats);
    }
}
