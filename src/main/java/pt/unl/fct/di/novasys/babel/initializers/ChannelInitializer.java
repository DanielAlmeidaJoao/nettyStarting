package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Properties;

public interface ChannelInitializer {

    NewIChannel initialize(BabelMessageSerializer serializer, ChannelListener<BabelMessage> list,
                           Properties properties, short protoId) throws IOException;
}
