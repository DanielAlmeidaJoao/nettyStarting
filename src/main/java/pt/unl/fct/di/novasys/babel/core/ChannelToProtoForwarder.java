package pt.unl.fct.di.novasys.babel.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.internal.*;
import quicSupport.utils.enums.TransmissionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelToProtoForwarder implements ChannelListener<BabelMessage> {

    private static final Logger logger = LogManager.getLogger(ChannelToProtoForwarder.class);

    final int channelId;
    final Map<Short, pt.unl.fct.di.novasys.babel.core.GenericProtocol> consumers;

    public ChannelToProtoForwarder(int channelId) {
        this.channelId = channelId;
        consumers = new ConcurrentHashMap<>();
    }

    public void addConsumer(short protoId, pt.unl.fct.di.novasys.babel.core.GenericProtocol consumer) {
        if (consumers.putIfAbsent(protoId, consumer) != null)
            throw new AssertionError("Consumer with protoId " + protoId + " already exists in channel");
    }

    @Override
    public void deliverMessage(BabelMessage message, Host host, String quicStreamId) {
        GenericProtocol channelConsumer = getConsumer(message.getDestProto());
        if(quicStreamId==null){
            channelConsumer.deliverMessageIn(new MessageInEvent(message, host, channelId));
        }else{
            channelConsumer.deliverQuicMessageIn(new QUICMessageInEvent(message, host, channelId,quicStreamId));
        }
    }
    @Override
    public void deliverMessage(byte [] message, Host host, String quicStreamId, short sourceProto, short destProto, short handlerId) {
        GenericProtocol channelConsumer = getConsumer(destProto);
        channelConsumer.deliverBytesIn(new BytesMessageInEvent(message,host,channelId,quicStreamId,sourceProto,destProto,handlerId));
    }

    @Override
    public int getChannelId() {
        return channelId;
    }

    private GenericProtocol getConsumer(short protoId){
        GenericProtocol channelConsumer;
        if (protoId == -1 && consumers.size() == 1)
            channelConsumer = consumers.values().iterator().next();
        else
            channelConsumer = consumers.get(protoId);
        if (channelConsumer == null) {
            logger.error("Channel " + channelId + " received message to protoId " +
                    protoId + " which is not registered in channel");
            throw new AssertionError("Channel " + channelId + " received message to protoId " +
                    protoId + " which is not registered in channel");
        }
        return channelConsumer;
    }
    @Override
    public void messageSent(BabelMessage addressedMessage, Host host, TransmissionType type) {
        consumers.values().forEach(c -> c.deliverMessageSent(new MessageSentEvent(addressedMessage, host, channelId)));
    }

    @Override
    public void messageFailed(BabelMessage addressedMessage, Host host, Throwable throwable, TransmissionType type) {
        consumers.values().forEach(c ->
                c.deliverMessageFailed(new MessageFailedEvent(addressedMessage, host, throwable, channelId)));
    }

    @Override
    public void deliverEvent(ChannelEvent channelEvent) {
        consumers.values().forEach(v -> v.deliverChannelEvent(new CustomChannelEvent(channelEvent, channelId)));
    }
}
