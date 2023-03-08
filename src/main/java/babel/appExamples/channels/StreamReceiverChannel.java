package babel.appExamples.channels;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.StreamReceiver;
import org.streamingAPI.server.StreamReceiverImplementation;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
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
    public final static String NAME = "TCPChannel";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";

    private ByteBuf tmp;
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
        new ChannelFuncHandlers(this::channelActive,this::channelReadConfigData,this::channelRead,this::channelClosed));
        try{
            streamReceiver.startListening(false);
        }catch (Exception e){
            throw new IOException(e);
        }
        tmp = Unpooled.buffer();
        currentLength = -1;
    }

    private byte [] prepend(byte [] data, short source, short dest){
        //8 -> length (4 bytes) + source (2 bytes) + dest (2 bytes)
        ByteBuf byteBuf = Unpooled.buffer(8+data.length);
        byteBuf.writeInt(data.length+4);
        byteBuf.writeShort(source);
        byteBuf.writeShort(dest);
        byteBuf.writeBytes(data);
        return byteBuf.array();
    }
    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        boolean triggerSent = false;
        BabelMessage babelMessage = (BabelMessage) msg;
        StreamMessage message = (StreamMessage) babelMessage.getMessage();
        Promise<Void> promise = streamReceiver.getDefaultEventExecutor().newPromise();
        promise.addListener(future -> {
            if (future.isSuccess() && triggerSent) listener.messageSent(msg, peer);
            else if (!future.isSuccess()) listener.messageFailed(msg, peer, future.cause());
        });
        byte [] bytes = prepend(message.getData(), babelMessage.getSourceProto(), babelMessage.getDestProto());
        streamReceiver.send(message.getStreamId(),bytes,bytes.length);
    }


    @Override
    public void closeConnection(Host peer, int connection) {

    }

    @Override
    public void openConnection(Host peer) {

    }

    private void channelActive(String channelId){

    }
    private void channelReadConfigData(String channelId, byte [] data){

    }
    private void channelRead(String channelId,byte [] data){
        if(data!=null){
            tmp.writeBytes(data);
        }
        if(currentLength==-1){
            if(data.length>=4){
                currentLength = tmp.readInt();
            }
        }
        if(tmp.readableBytes()>=currentLength){
            short source = tmp.readShort();
            short dest = tmp.readShort();
            byte [] appData = new byte[currentLength-4];
            tmp.readBytes(appData);

            StreamMessage streamMessage = new StreamMessage(appData,appData.length,channelId);
            BabelMessage babelMessage = new BabelMessage(streamMessage,source,dest);
            listener.deliverMessage((T) babelMessage,self);

            currentLength=-1;
            channelRead(channelId,null);
        }
    }
    private void channelClosed(String channelId){
        tmp.release();
    }
}
