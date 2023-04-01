package babel.appExamples.channels.initializers;

import babel.appExamples.channels.BabelStreamingChannel;
import lombok.SneakyThrows;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

public class BabelStreamInitializer implements ChannelInitializer<BabelStreamingChannel<BabelMessage>> {
    @Override
    public BabelStreamingChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelStreamingChannel<>(serializer, list, properties);
    }
}
