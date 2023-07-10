package appExamples2.appExamples.channels.babelQuicChannel;


import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.NetworkProtocol;

import java.io.IOException;
import java.util.Properties;

public class BabelQuicInitializer implements ChannelInitializer<NewIChannel<BabelMessage>> {
    /**
    @Override
    public NewIChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelQUIC_TCP_Channel<>(serializer, list, properties);
    }**/
    @Override
    public NewIChannel<BabelMessage> initialize(BabelMessageSerializerInterface<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelQUIC_TCP_ChannelWithControlledClose<>(serializer, list, properties,protoId, NetworkProtocol.QUIC);
    }
}
