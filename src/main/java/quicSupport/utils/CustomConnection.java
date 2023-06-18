package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.enums.TransmissionType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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

    private final ConnectionId connectionId;
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
        addStream(remote,defaultStream);
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
    public void addStream(ConnectionId other,QuicStreamChannel streamChannel){
        if(!this.connectionId.address.equals(other.address)){
            throw new AssertionError("TRYING TO ADD STREAM FROM A DIFFERENT HOST "+this.connectionId.address+" VS "+other.address);
        }
        streams.put(other.linkId,streamChannel);
    }
    public QuicStreamChannel getStream(String id){
        QuicStreamChannel ele = streams.get(id);
        if(ele == null && brothers != null){
            for (CustomConnection brother : brothers) {
                ele = brother.getStream(id);
                if(ele!=null){
                    return ele;
                }
            }
        }
        return streams.get(id);
    }
    public CustomConnection broReplacer(){
        while (brothers!=null && !brothers.isEmpty()){
            CustomConnection gg = brothers.remove();
            if(!gg.connectionDown()){
                return gg;
            }
        }
        return null;
    }
    public CustomConnection closeStream(String streamId)  {
        try{
            QuicStreamChannel streamChannel = streams.remove(streamId);
            if(defaultStream==streamChannel){
                if(streams.isEmpty() && brothers != null){
                    CustomConnection boss = brothers.remove();
                    if(boss != null){
                        boss.setBrothers(brothers);
                        brothers = null;
                        return boss;
                    }
                }else if(!streams.isEmpty()){
                    defaultStream = streams.entrySet().iterator().next().getValue();
                    return this;
                }
            }
            streamChannel.shutdown();
            streamChannel.disconnect();
        }catch (Exception e){
            e.printStackTrace();
        }
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
        return connection.isTimedOut() || connection.isActive() || !connection.isOpen();
    }
}
