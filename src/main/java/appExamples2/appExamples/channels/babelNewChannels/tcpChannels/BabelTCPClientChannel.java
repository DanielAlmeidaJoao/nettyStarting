package appExamples2.appExamples.channels.babelNewChannels.tcpChannels;

import appExamples2.appExamples.channels.babelNewChannels.BabelQUIC_TCP_ChannelWithControlledClose;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public class BabelTCPClientChannel extends BabelQUIC_TCP_ChannelWithControlledClose{
    public final static String CHANNEL_NAME = "BabelTCPClientChannel";

    public BabelTCPClientChannel(BabelMessageSerializer serializer, ChannelListener list, Properties properties,
                                 short protoId) throws IOException {
        super(serializer, list, properties, protoId,NetworkProtocol.TCP,NetworkRole.CLIENT);
    }
}
