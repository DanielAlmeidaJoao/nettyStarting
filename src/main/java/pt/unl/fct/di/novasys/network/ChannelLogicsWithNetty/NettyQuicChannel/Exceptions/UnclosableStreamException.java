package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.Exceptions;

public class UnclosableStreamException extends Exception{

    public UnclosableStreamException(String msg){
        super(msg);
    }
}
