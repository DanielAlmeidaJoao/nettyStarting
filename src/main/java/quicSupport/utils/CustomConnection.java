package quicSupport.utils;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
public class CustomConnection {
    private final QuicChannel connection;
    private Map<String,QuicStreamChannel> streams;
    private final QuicStreamChannel defaultStream;
    private final boolean  inComing;

    private InetSocketAddress remote;

    public CustomConnection(QuicStreamChannel quicStreamChannel,InetSocketAddress remote, boolean inComing){
        defaultStream = quicStreamChannel;
        connection = defaultStream.parent();
        this.inComing = inComing;
        streams = new HashMap<>();
        this.remote = remote;
    }
    public void addStream(QuicStreamChannel streamChannel){
        streams.put(streamChannel.id().asShortText(),streamChannel);
    }
    public QuicStreamChannel getStream(String id){
        return streams.get(id);
    }
    public void close(){

    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
