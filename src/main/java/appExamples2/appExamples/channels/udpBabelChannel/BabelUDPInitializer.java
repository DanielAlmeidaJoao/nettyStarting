package appExamples2.appExamples.channels.udpBabelChannel;


import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Properties;

public class BabelUDPInitializer implements ChannelInitializer<BabelUDPChannel<BabelMessage>> {
    @Override
    public BabelUDPChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list, Properties properties, short protoId) throws IOException {
        return new BabelUDPChannel<>(serializer, list, properties);
    }
}
