package pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.tcpChannels;

import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.BabelQUIC_TCP_ChannelWithControlledClose;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkProtocol;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public class BabelTCP_P2P_Channel extends BabelQUIC_TCP_ChannelWithControlledClose {
    public final static String CHANNEL_NAME = "BabelTCP_P2P_Channel";
    public BabelTCP_P2P_Channel(BabelMessageSerializer serializer, ChannelListener list, Properties properties, short protoId) throws IOException {
        super(serializer, list, properties, protoId, NetworkProtocol.TCP, NetworkRole.P2P_CHANNEL);
    }
}
