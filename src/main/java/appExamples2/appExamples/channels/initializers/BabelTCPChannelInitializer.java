package appExamples2.appExamples.channels.initializers;

import appExamples2.appExamples.channels.babelNewChannels.tcpChannels.BabelTCPClientChannel;
import appExamples2.appExamples.channels.babelNewChannels.tcpChannels.BabelTCPServerChannel;
import appExamples2.appExamples.channels.babelNewChannels.tcpChannels.BabelTCP_P2P_Channel;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public class BabelTCPChannelInitializer implements ChannelInitializer<NewIChannel<BabelMessage>> {
    private final NetworkRole networkRole;
    public BabelTCPChannelInitializer(NetworkRole role){
        networkRole = role;
    }
    @Override
    public NewIChannel<BabelMessage> initialize(BabelMessageSerializerInterface<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        switch (networkRole){
            case CLIENT: return new BabelTCPClientChannel(serializer, list, properties,protoId);
            case SERVER: return new BabelTCPServerChannel(serializer, list, properties,protoId);
            case P2P_CHANNEL: return new BabelTCP_P2P_Channel(serializer, list, properties,protoId);
            default: throw new RuntimeException("UNKWON NETWORK ROLE");
        }
    }
}
