package babel.appExamples.channels.initializers;

import babel.appExamples.channels.StreamSenderChannel;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleClientChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

public class StreamSenderInitializers implements ChannelInitializer<StreamSenderChannel<BabelMessage>> {
    @Override
    public StreamSenderChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new StreamSenderChannel<>(serializer,list,properties);
    }
}
