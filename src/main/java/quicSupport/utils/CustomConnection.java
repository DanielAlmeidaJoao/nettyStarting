package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnclosableStreamException;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class CustomConnection {

    private static final Logger logger = LogManager.getLogger(CustomConnection.class);

    private final QuicChannel connection;
    private final QuicStreamChannel defaultStream;
    private final boolean  inComing;
    private Map<String,QuicStreamChannel> streams;
    private ScheduledFuture scheduledFuture;

    private InetSocketAddress remote;
    private boolean canSendHeartBeat;

    public CustomConnection(QuicStreamChannel quicStreamChannel,InetSocketAddress remote, boolean inComing){
        defaultStream = quicStreamChannel;
        connection = defaultStream.parent();
        this.inComing = inComing;
        streams = new HashMap<>();
        this.remote = remote;
        addStream(defaultStream);
        scheduledFuture = null;
        canSendHeartBeat = inComing;
        serverStartScheduling();
        //logger.info("CONNECTION TO {} ON. DEFAULT STREAM: {} .",remote,defaultStream.id().asShortText());
    }
    public void addStream(QuicStreamChannel streamChannel){
        streams.put(streamChannel.id().asShortText(),streamChannel);
    }
    public QuicStreamChannel getStream(String id){
        return streams.get(id);
    }

    public void closeStream(String streamId) throws UnclosableStreamException {
        QuicStreamChannel streamChannel = streams.get(streamId);
        if(defaultStream==streamChannel){
            throw new UnclosableStreamException("DEFAULT STREAM <"+streamId+"> CANNOT BE CLOSED.");
        }
        streamChannel.shutdown();
        streamChannel.disconnect();
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
            scheduledFuture = defaultStream.eventLoop().schedule(() -> {
                //logger.debug("HEART BEAT SENT TO {}",remote);
                defaultStream.writeAndFlush(QUICLogics.writeBytes(1,"a".getBytes(), QUICLogics.KEEP_ALIVE));
            }, (long) (QUICLogics.maxIdleTimeoutInSeconds*0.75), TimeUnit.SECONDS);
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
