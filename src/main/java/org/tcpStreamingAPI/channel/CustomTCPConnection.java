package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.ConnectionOrStreamType;

public class CustomTCPConnection {
    public final Channel channel;
    public final ConnectionOrStreamType type;
    public CustomTCPConnection(Channel channel,ConnectionOrStreamType type){
        this.channel=channel;
        this.type=type;
    }
}
