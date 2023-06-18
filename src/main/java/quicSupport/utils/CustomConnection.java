package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnclosableStreamException;
import quicSupport.utils.enums.TransmissionType;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class CustomConnection {
    private static final Logger logger = LogManager.getLogger(CustomConnection.class);
    private final QuicChannel connection;
    private QuicStreamChannel defaultStream;
    private final boolean  inComing;
    private Map<String,QuicStreamChannel> streams;
    private ScheduledFuture scheduledFuture;
    public TransmissionType transmissionType;

    private ConnectionId connectionId;
    private boolean canSendHeartBeat;
    private static long heartBeatTimeout;
    private final long creationTime;
    private Queue<CustomConnection> brothers;

    public CustomConnection(QuicStreamChannel quicStreamChannel, ConnectionId remote, boolean inComing, boolean withHeartBeat, long heartBeatTimeout, TransmissionType type){
        creationTime = System.currentTimeMillis();
        defaultStream = quicStreamChannel;
        connection = defaultStream.parent();
        this.inComing = inComing;
        streams = new HashMap<>();
        this.connectionId = remote;
        addStream(defaultStream);
        scheduledFuture = null;
        canSendHeartBeat = inComing;
        this.heartBeatTimeout = heartBeatTimeout;
        transmissionType = type;
        if(inComing&&withHeartBeat){
            serverStartScheduling();
        }
        //logger.info("CONNECTION TO {} ON. DEFAULT STREAM: {} .",remote,defaultStream.id().asShortText());
    }
    public void addConnection(CustomConnection customConnection){
        if(brothers == null){
            brothers = new LinkedList<>();
        }
        brothers.add(customConnection);
    }
    public void closeAllBros(){
        if(brothers !=null){
            for (CustomConnection customConnection : brothers) {
                customConnection.close();
            }
        }
        brothers = null;
    }
    public void setBrothers(Queue<CustomConnection> bros){
        assert brothers == null;
        brothers = bros;
    }
    public void removeBro(String conId){
        for (CustomConnection brother : brothers) {
            if(brother.getConnectionId().linkId == conId){
                brothers.remove(brother);
                brother.close();
            }
        }
    }
    public boolean hasPassedOneSec(){
        return (System.currentTimeMillis()-creationTime)>2000;
    }
    public void addStream(QuicStreamChannel streamChannel){
        streams.put(streamChannel.id().asShortText(),streamChannel);
    }
    public QuicStreamChannel getStream(String id){
        return streams.get(id);
    }

    public CustomConnection closeStream(String streamId) throws UnclosableStreamException {
        QuicStreamChannel streamChannel = streams.remove(streamId);
        if(defaultStream==streamChannel){
            if(streams.isEmpty()){
                CustomConnection boss = brothers.remove();
                if(boss != null){
                    boss.setBrothers(brothers);
                    return boss;
                }
            }else{
                defaultStream = streams.entrySet().iterator().next().getValue();
            }
        }
        streamChannel.shutdown();
        streamChannel.disconnect();
        return null;
    }
    public void close(){
        //streams = null;
        connection.disconnect();
        connection.close();
        closeAllBros();
    }

    private void serverStartScheduling(){
        if(inComing){
            scheduleSendHeartBeat_KeepAlive();
            canSendHeartBeat=false;
        }
    }

    public void scheduleSendHeartBeat_KeepAlive(){
            if(scheduledFuture!=null){
                scheduledFuture.cancel(true);
            }
            scheduledFuture = defaultStream.eventLoop().schedule(() -> {
                logger.info("HEART BEAT SENT TO {}",connectionId.address);
                defaultStream.writeAndFlush(QUICLogics.writeBytes(1,"a".getBytes(), QUICLogics.KEEP_ALIVE, transmissionType));
            }, (long) (heartBeatTimeout*0.75), TimeUnit.SECONDS);
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
