package appExamples2.appExamples.channels.initializers;

import appExamples2.appExamples.channels.newTCPChannel.BabelNewTCPChannel;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Properties;

public class BabelStreamInitializer implements ChannelInitializer<NewIChannel<BabelMessage>> {
    @Override
    public NewIChannel<BabelMessage> initialize(BabelMessageSerializerInterface<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelNewTCPChannel<>(serializer, list, properties,protoId);
    }
}
