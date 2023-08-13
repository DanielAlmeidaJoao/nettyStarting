package appExamples2.appExamples.channels.babelNewChannels.quicChannels;

import appExamples2.appExamples.channels.babelNewChannels.BabelQUIC_TCP_ChannelWithControlledClose;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public class BabelQUICServerChannel<T> extends BabelQUIC_TCP_ChannelWithControlledClose<T> {
    public final static String CHANNEL_NAME = "BabelQUICServerChannel";

    public BabelQUICServerChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short protoId) throws IOException {
        super(serializer, list, properties, protoId, NetworkProtocol.QUIC,  NetworkRole.SERVER);
    }
}
