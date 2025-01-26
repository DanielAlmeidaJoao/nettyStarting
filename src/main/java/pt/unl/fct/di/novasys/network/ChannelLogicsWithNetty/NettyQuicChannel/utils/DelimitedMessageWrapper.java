package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils;

public class DelimitedMessageWrapper {
    public final int len;
    public final byte [] data;
    public final byte msgCode;


    public DelimitedMessageWrapper(int len, byte[] data, byte msgCode) {
        this.len = len;
        this.data = data;
        this.msgCode = msgCode;
    }
}
