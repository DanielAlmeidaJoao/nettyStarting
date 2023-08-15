package pt.unl.fct.di.novasys.babel.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;
import udpSupport.metrics.UDPNetworkStatsWrapper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.NoSuchElementException;

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

    protected void sendMessage(int channelId,byte[] data,int dataLen, Host dest, short sourceProto, short destProto){
        if (logger.isDebugEnabled())
            logger.debug("Sending: bytes to " + dest + " proto " + destProto +
                    " channel " + channelId);
        babel.sendMessage(channelId,data,dataLen, dest, sourceProto, destProto);
    }
    protected void sendMessage(int channelId,byte[] data,int dataLen, String streamId, short sourceProto, short destProto){
        if (logger.isDebugEnabled())
            logger.debug("Sending: bytes to " + streamId + " proto " + destProto +
                    " channel " + channelId);
        babel.sendMessage(channelId,data,dataLen,streamId,sourceProto,destProto);
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
    protected TransmissionType getConnectionType(int channelId, String streamId) throws NoSuchElementException {
        return babel.getConnectionType(channelId,streamId);
    }
    protected NetworkProtocol getNetworkProtocol(int channelId){
        return babel.getNetworkProtocol(channelId);
    }
    protected NetworkRole getNetworkRole(int channelId){
        return babel.getNetworkRole(channelId);
    }

    protected List<ConnectionProtocolMetrics> getCurrentMetrics(int channelId){
        return babel.getCurrentMetrics(channelId);
    }

    protected List<ConnectionProtocolMetrics> getOldMetrics(int channelId){
        return babel.getOldMetrics(channelId);
    }
    protected List<UDPNetworkStatsWrapper> getUDPMetrics(int channelId){
        return babel.getUDPMetrics(channelId);
    }
    protected void shutDownChannel(int channelId, short protoId){
        getChannelOrThrow(channelId);
        if(babel.closeChannel(channelId,protoId)){
            channels.remove(channelId);
        }
    }
}
