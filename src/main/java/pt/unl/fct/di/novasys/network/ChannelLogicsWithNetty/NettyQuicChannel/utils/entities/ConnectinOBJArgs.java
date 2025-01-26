package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.entities;

import lombok.AllArgsConstructor;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;

@AllArgsConstructor
public class ConnectinOBJArgs {

    public final String conId;
    public final TransmissionType type;
    public final short source,dest;

}
