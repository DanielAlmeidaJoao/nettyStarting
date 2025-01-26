package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageToByteEncoderParameter {
    public final byte msgCode;
    public final byte [] data;
    public final int dataLen;
}
