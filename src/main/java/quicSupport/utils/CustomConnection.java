package quicSupport.utils;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.CustomQuicChannel;
import quicSupport.UnknownElement;

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
        addStream(defaultStream);//TO DO: REMOVE THIS LINE
        scheduledFuture = null;
        canSendHeartBeat = inComing;
        serverStartScheduling();
        logger.info("CONNECTION TO {} ON. DEFAULT STREAM: {} .",remote,defaultStream.id().asShortText());
    }
    public void addStream(QuicStreamChannel streamChannel){
        streams.put(streamChannel.id().asShortText(),streamChannel);
    }
    public QuicStreamChannel getStream(String id){
        return streams.get(id);
    }

    public void closeStream(String streamId) throws UnknownElement{
        QuicStreamChannel streamChannel = streams.get(streamId);
        if(streamChannel==null){
            throw new UnknownElement("UNKNOWN STREAM: "+streamId);
        }
        streamChannel.shutdown();
        streamChannel.disconnect();
    }
    public void close(){
        streams = null;
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
                logger.info("HEART BEAT SENT. INCOMING ? {}",inComing);
                defaultStream.writeAndFlush(Logics.writeBytes(1,"a".getBytes(),Logics.KEEP_ALIVE));
            }, (long) (Logics.maxIdleTimeoutInSeconds*0.75), TimeUnit.SECONDS);
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
