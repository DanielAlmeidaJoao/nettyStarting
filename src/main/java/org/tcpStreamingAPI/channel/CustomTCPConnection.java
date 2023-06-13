package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.TransmissionType;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public CustomTCPConnection(Channel channel, TransmissionType type){
        this.channel=channel;
        this.type=type;
    }
}
