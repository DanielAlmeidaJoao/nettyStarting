package babel.appExamples.channels;

import babel.appExamples.channels.messages.StreamMessage;
import org.streamingAPI.server.StreamReceiver;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

public class StreamReceiverChannel<T> implements IChannel<T> {

    private Host self;
    public final static String NAME = "TCPChannel";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";


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
        /** streamReceiver = new StreamReceiverImplementation(addr.getHostName(),port,
        new HandlerFunctions(this::channelActive,this::channelReadConfigData,this::channelRead,this::channelClosed));**/
        streamReceiver = null;
        this.listener = list;
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        BabelMessage babelMessage = (BabelMessage) msg;
        StreamMessage message = (StreamMessage) babelMessage.getMessage();
        streamReceiver.sendBytes(message.getStreamId(), message.getData(), message.getDataLength());
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
        StreamMessage streamMessage = new StreamMessage(data,data.length,channelId);
        listener.deliverMessage((T) streamMessage,self);
    }
    private void channelClosed(String channelId){

    }
}
