package appExamples2.appExamples.channels;

import pt.unl.fct.di.novasys.network.data.Host;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

@FunctionalInterface
public interface StreamDeliveredHandlerFunction {
    void execute(BabelOutputStream babelOutputStream, Host host, String quicStreamId,short destProto,int channelId,BabelInputStream inputStream);
}
