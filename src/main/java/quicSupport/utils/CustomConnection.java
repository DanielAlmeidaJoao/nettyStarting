package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnclosableStreamException;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class CustomConnection {
    private static final Logger logger = LogManager.getLogger(CustomConnection.class);
    private final QuicChannel connection;
    private final CustomQUICStream defaultStream;
    private final boolean  inComing;
    private Map<String,CustomQUICStream> streams;
    private ScheduledFuture scheduledFuture;
    public TransmissionType transmissionType;

    private InetSocketAddress remote;
    private boolean canSendHeartBeat;
    private static long heartBeatTimeout;
    private final long creationTime;

    public CustomConnection(CustomQUICStream quicStreamChannel,InetSocketAddress remote, boolean inComing, boolean withHeartBeat, long heartBeatTimeout, TransmissionType type){
        creationTime = System.currentTimeMillis();
        defaultStream = quicStreamChannel;
        connection = defaultStream.streamChannel.parent();
        this.inComing = inComing;
        streams = new HashMap<>();
        this.remote = remote;
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
    public boolean hasPassedOneSec(){
        return (System.currentTimeMillis()-creationTime)>2000;
    }
    public void addStream(CustomQUICStream streamChannel){
        streams.put(streamChannel.streamChannel.id().asShortText(),streamChannel);
    }
    public CustomQUICStream getStream(String id){
        return streams.get(id);
    }

    public void closeStream(String streamId) throws UnclosableStreamException {
        CustomQUICStream streamChannel = streams.get(streamId);
        if(defaultStream.streamChannel==streamChannel.streamChannel){
            throw new UnclosableStreamException("DEFAULT STREAM <"+streamId+"> CANNOT BE CLOSED.");
        }
        streamChannel.streamChannel.shutdown();
        streamChannel.streamChannel.disconnect();
    }
    public void close(){
        //streams = null;
        connection.disconnect();
        connection.close();
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
            scheduledFuture = defaultStream.streamChannel.eventLoop().schedule(() -> {
                logger.info("HEART BEAT SENT TO {}",remote);
                defaultStream.streamChannel.writeAndFlush(QUICLogics.writeBytes(1,"a".getBytes(), QUICLogics.KEEP_ALIVE, transmissionType));
            }, (long) (heartBeatTimeout*0.75), TimeUnit.SECONDS);
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
