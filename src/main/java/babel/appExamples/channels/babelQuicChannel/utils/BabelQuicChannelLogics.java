package babel.appExamples.channels.babelQuicChannel.utils;

import pt.unl.fct.di.novasys.network.data.Host;

import java.net.InetSocketAddress;

public class BabelQuicChannelLogics {

    public static Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }


    public static InetSocketAddress toInetSOcketAddress(Host address){
        return new InetSocketAddress(address.getAddress(),address.getPort());
    }
}
