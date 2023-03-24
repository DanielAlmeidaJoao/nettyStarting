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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
public class CustomConnection {

    private static final Logger logger = LogManager.getLogger(CustomConnection.class);

    private final QuicChannel connection;
    private final QuicStreamChannel defaultStream;
    private final boolean  inComing;
    private Map<String,QuicStreamChannel> streams;


    private InetSocketAddress remote;

    public CustomConnection(QuicStreamChannel quicStreamChannel,InetSocketAddress remote, boolean inComing){
        defaultStream = quicStreamChannel;
        connection = defaultStream.parent();
        this.inComing = inComing;
        streams = new HashMap<>();
        this.remote = remote;
        addStream(defaultStream);//TO DO: REMOVE THIS LINE
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
        if(!connection.isTimedOut()){
            connection.disconnect();
            connection.close();
        }
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
