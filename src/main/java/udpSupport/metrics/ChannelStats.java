package udpSupport.metrics;

import lombok.Getter;
import org.modelmapper.ModelMapper;
import quicSupport.utils.metrics.QuicConnectionMetrics;
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

    public void addSentBytes(InetSocketAddress peer, long bytes, byte message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        switch (message_code){
            case UDPLogics.APP_MESSAGE: networkStats.getMessageStats().addBytesSent(bytes);break;
            case UDPLogics.APP_ACK: networkStats.getAckStats().addBytesSent(bytes);break;
        }
    }
    public void addReceivedBytes(InetSocketAddress peer, long bytes,byte message_code){
        NetworkStatsWrapper networkStats = statsMap.computeIfAbsent(peer,address -> new NetworkStatsWrapper(peer));
        switch (message_code){
            case UDPLogics.APP_MESSAGE: networkStats.getMessageStats().addBytesReceived(bytes);break;
            case UDPLogics.APP_ACK: networkStats.getAckStats().addBytesReceived(bytes);break;
        }
    }
    private NetworkStats clone(NetworkStats stats){
        return UDPLogics.modelMapper.map(stats,NetworkStats.class);
    }
    public ChannelStats cloneChannelMetric(){
        Map<InetSocketAddress,NetworkStatsWrapper> stats = new HashMap<>(statsMap.size());
        for (NetworkStatsWrapper value : statsMap.values()) {
            NetworkStatsWrapper copy = new NetworkStatsWrapper(value.getDest(),clone(value.getMessageStats()),clone(value.getAckStats()));
            stats.put(value.getDest(),copy);
        }
        return new ChannelStats(stats);
    }
}
