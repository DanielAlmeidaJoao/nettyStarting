package appExamples2.appExamples.channels.babelQuicChannel;


import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Properties;

public class BabelQuicInitializer implements ChannelInitializer<BabelQuicChannel<BabelMessage>> {
    @Override
    public BabelQuicChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelQuicChannel<>(serializer, list, properties);
    }
}
