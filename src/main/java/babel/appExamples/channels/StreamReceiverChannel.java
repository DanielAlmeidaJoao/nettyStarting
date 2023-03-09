package babel.appExamples.channels;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.StreamReceiver;
import org.streamingAPI.server.StreamReceiverImplementation;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;

public class StreamReceiverChannel<T> implements IChannel<T> {
    private static final Logger logger = LogManager.getLogger(StreamReceiverChannel.class);

    private Host self;
    public final static String NAME = "STREAM_RECEIVER";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";

    private int currentLength;


    private Map<Host,String> streams;
    private final StreamReceiver streamReceiver;
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
        streamReceiver = new StreamReceiverImplementation(addr.getHostName(),port,
        new ChannelFuncHandlers(this::channelActive,this::channelReadConfigData,this::channelReadByteBuf,this::channelClosed));
        try{
            streamReceiver.startListening(false,true);
        }catch (Exception e){
            throw new IOException(e);
        }
        currentLength = -1;
    }

    public static ByteBuf prepend(byte [] data, short source, short dest){
        //8 -> length (4 bytes) + source (2 bytes) + dest (2 bytes)
        ByteBuf byteBuf = Unpooled.buffer();
        //byteBuf.writeInt(data.length);
        //byteBuf.writeInt(source);
        //byteBuf.writeInt(dest);
        byteBuf.writeBytes(data);
        return byteBuf;
    }
    @Override
    public void sendMessage(T msg, Host peer, int connection) {}

    @Override
    public void closeConnection(Host peer, int connection) {}

    @Override
    public void openConnection(Host peer) {

    }

    private void channelActive(String channelId){

    }
    private void channelReadConfigData(String channelId, byte [] data){

    }
    int total =0;
    private void channelReadByteBuf(String streamId, byte [] bytes){
        //System.out.println("CALLED!!");
        /**
        int source = byteBuf.readInt();
        int dest = byteBuf.readInt();
        byte [] appData = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(appData);
        StreamMessage streamMessage = new StreamMessage(appData,appData.length,streamId);
        //listener.deliverMessage((T) babelMessage,self);
         **/
        //BabelMessage babelMessage = new BabelMessage(byteBuf, (short) 206, (short) 206);
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        total+=buf.readableBytes()-4;

        short src = buf.readShort();
        short dest=buf.readShort();

        System.out.println("TOTAL "+total+" "+bytes.length+" src: "+src+" dest "+dest);
        buf.release();
    }
    private void channelClosed(String channelId){
        Throwable cause = new Throwable("CLOSED ???");
        listener.deliverEvent(new InConnectionDown(self, cause));
    }
}
