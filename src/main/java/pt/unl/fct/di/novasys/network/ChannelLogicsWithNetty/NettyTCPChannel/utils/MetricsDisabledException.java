package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils;

public class MetricsDisabledException extends Exception{

    public MetricsDisabledException(String msg){
        super(msg);
    }
}
