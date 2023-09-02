package appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel;


import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;

import java.io.IOException;
import java.util.Properties;

public class BabelUDPInitializer implements ChannelInitializer{
    @Override
    public BabelUDPChannel initialize(BabelMessageSerializer serializer, ChannelListener list, Properties properties, short protoId) throws IOException {
        return new BabelUDPChannel(serializer, list, properties,protoId);
    }
}
