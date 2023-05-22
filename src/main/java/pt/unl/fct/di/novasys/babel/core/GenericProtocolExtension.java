package pt.unl.fct.di.novasys.babel.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.net.InetSocketAddress;

public abstract class GenericProtocolExtension extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(GenericProtocolExtension.class);

    public GenericProtocolExtension(String protoName, short protoId) {
        super(protoName, protoId);
    }

    protected final void sendMessage(ProtoMessage msg, String streamId) {
        sendMessage(defaultChannel, msg, this.protoId, streamId);
    }

    protected final void sendMessage(int channelId, ProtoMessage msg, short destProto, String streamId) {
        getChannelOrThrow(channelId);
        if (logger.isDebugEnabled())
            logger.debug("Sending: " + msg + " to " + streamId + " proto " + destProto +
                    " channel " + channelId);
        babel.sendMessage(channelId, this.protoId, new BabelMessage(msg, this.protoId, destProto), streamId);
    }

    protected final void createStream(Host dest) {
        createStream(defaultChannel, this.protoId, dest);
    }

    protected final void createStream(int channelId, short proto, Host dest) {
        getChannelOrThrow(channelId);
        if (logger.isDebugEnabled())
            logger.debug("CREATING A STREAM TO {} IN CHANNEL {}", dest, channelId);
        babel.createStream(channelId, proto, dest);
    }

    protected final void closeStream(String streamId) {
        closeStream(defaultChannel, this.protoId, streamId);
    }

    protected final void closeStream(int channelId, short proto, String streamId) {
        getChannelOrThrow(channelId);
        if (logger.isDebugEnabled())
            logger.debug("CLOSING STREAM {} IN CHANNEL {}", streamId, channelId);
        babel.closeStream(channelId, proto, streamId);
    }
    protected boolean isConnected(int channelId, Host peer) {
        getChannelOrThrow(channelId);
        return babel.isConnected(channelId, peer);
    }
    protected String [] getStreams(int channelId){
        getChannelOrThrow(channelId);
        return babel.getStreams(channelId);
    }
    protected InetSocketAddress[] getConnections(int channelId){
        getChannelOrThrow(channelId);
        return babel.getConnections(channelId);
    }
    protected int numConnectedPeers(int channelId){
        getChannelOrThrow(channelId);
        return babel.connectedPeers(channelId);
    }
}
