package babel.appExamples.channels;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.connectionSetups.StreamInConnection;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

public class StreamReceiverChannel<T> implements IChannel<T> {
    private static final Logger logger = LogManager.getLogger(StreamReceiverChannel.class);

    private Host self;
    public final static String NAME = "STREAM_RECEIVER";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";

    private Map<Host,String> streams;
    private final StreamInConnection streamReceiver;
    private final ChannelListener<T> listener;

    public StreamReceiverChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties)  throws IOException {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new Host(addr,port);
        this.listener = list;
        streamReceiver = null; /** new StreamInConnection(addr.getHostName(),port,
        new ChannelFuncHandlers(this::channelActive,
                this::channelReadConfigData,
                this::channelRead,
                this::channelClosed
        ));**/
        try{
            streamReceiver.startListening(false,true,null,null);
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {}

    @Override
    public void closeConnection(Host peer, int connection) {}

    @Override
    public void openConnection(Host peer) {

    }

    private void channelActive(Channel channel, HandShakeMessage handShakeMessage){

    }
    private void channelReadConfigData(String channelId, byte [] data){

    }

    int total = 0;
    private void channelRead(String streamId, byte [] bytes){

        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        int dataLen=buf.readableBytes()-4;
        short src = buf.readShort();
        short dest=buf.readShort();
        byte [] appData = new byte[dataLen];
        buf.readBytes(appData,0,dataLen);

        total +=dataLen;
        StreamMessage streamMessage = new StreamMessage(appData,dataLen,streamId);
        BabelMessage babelMessage = new BabelMessage(streamMessage,src,dest);
        listener.deliverMessage((T) babelMessage,self);
    }
    private void channelClosed(String channelId){
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",channelId));
        listener.deliverEvent(new InConnectionDown(self, cause));
    }
}
