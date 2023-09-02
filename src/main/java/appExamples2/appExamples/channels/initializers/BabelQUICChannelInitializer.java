package appExamples2.appExamples.channels.initializers;


import appExamples2.appExamples.channels.babelNewChannels.quicChannels.BabelQUICClientChannel;
import appExamples2.appExamples.channels.babelNewChannels.quicChannels.BabelQUICServerChannel;
import appExamples2.appExamples.channels.babelNewChannels.quicChannels.BabelQUIC_P2P_Channel;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public class BabelQUICChannelInitializer implements ChannelInitializer{
    private final NetworkRole networkRole;
    public BabelQUICChannelInitializer(NetworkRole role){
        networkRole = role;
    }
    @Override
    public NewIChannel initialize(BabelMessageSerializer serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        switch (networkRole){
            case CLIENT: return new BabelQUICClientChannel(serializer, list, properties,protoId);
            case SERVER: return new BabelQUICServerChannel(serializer, list, properties,protoId);
            case P2P_CHANNEL: return new BabelQUIC_P2P_Channel(serializer, list, properties,protoId);
            default: throw new RuntimeException("UNKWON NETWORK ROLE");
        }
    }
}
