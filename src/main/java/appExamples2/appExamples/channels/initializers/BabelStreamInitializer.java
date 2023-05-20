package appExamples2.appExamples.channels.initializers;

import appExamples2.appExamples.channels.streamingChannel.BabelStreamingChannel;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Properties;

public class BabelStreamInitializer implements ChannelInitializer<BabelStreamingChannel<BabelMessage>> {
    @Override
    public BabelStreamingChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelStreamingChannel<>(serializer, list, properties);
    }
}
