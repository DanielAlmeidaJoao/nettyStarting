package udpSupport.metrics;

import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
public class NetworkStatsWrapper {
    private InetSocketAddress dest;

    private Map<NetworkStatsKindEnum,NetworkStats> statsMap;
    public NetworkStatsWrapper(InetSocketAddress host){
        statsMap = new HashMap<>(3);
        statsMap.put(NetworkStatsKindEnum.MESSAGE_STATS,new NetworkStats("totalMessageStats"));
        statsMap.put(NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED, new NetworkStats("effectiveMessageStats"));
        statsMap.put(NetworkStatsKindEnum.ACK_STATS,new NetworkStats("ackStats"));
        dest=host;
    }

    public NetworkStats getStats(NetworkStatsKindEnum key) {
        return statsMap.get(key);
    }
    public Collection<NetworkStats> statsCollection(){
        return statsMap.values();
    }
    public NetworkStatsWrapper(InetSocketAddress host, Map<NetworkStatsKindEnum,NetworkStats> statsMap){
        this.statsMap=statsMap;
        dest=host;
    }

}
