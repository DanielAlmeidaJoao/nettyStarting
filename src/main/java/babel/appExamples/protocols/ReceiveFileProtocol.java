package babel.appExamples.protocols;

import babel.appExamples.channels.StreamReceiverChannel;
import babel.appExamples.channels.messages.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;

public class ReceiveFileProtocol extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(ReceiveFileProtocol.class);
    public static final short ID = 204;
    private Host self;
    FileOutputStream fos;
    private int channelId;
    public ReceiveFileProtocol(Properties props) throws IOException {
        super(ReceiveFileProtocol.class.getSimpleName(),ID);
        String address = props.getProperty("address");
        String port = props.getProperty("p2p_port");
        logger.info("Listening on {}:{}", address, port);
        this.self = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        channelId = createChannel(StreamReceiverChannel.NAME, props);
        try {
            fos = new FileOutputStream("DANIEL.mp4");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        registerMessageSerializer(channelId, StreamMessage.ID,StreamMessage.serializer);
        registerMessageHandler(channelId,StreamMessage.ID,this::uponReceiveMessage);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        try {
            System.out.println("CONNECTION CLOSED! "+totoal);
            fos.close();
        }catch (Exception e){e.printStackTrace();};
    }
    int totoal = 0;
    private void uponReceiveMessage(StreamMessage msg, Host from, short sourceProto, int channelId){
        try{
            totoal +=msg.getDataLength();
            fos.write(msg.getData(), 0,msg.getDataLength());
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
