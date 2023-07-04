package appExamples2.appExamples.protocols;

import appExamples2.appExamples.channels.messages.EndOfStreaming;
import appExamples2.appExamples.channels.messages.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class ReceiveFileProtocol extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(ReceiveFileProtocol.class);
    public static final short ID = 204;
    private Host self;
    private Host forwarder;
    FileOutputStream fos;
    private int channelId;
    public ReceiveFileProtocol(Properties props) throws IOException {
        super(ReceiveFileProtocol.class.getSimpleName(),ID);
        String address = props.getProperty("address");
        String port = props.getProperty("p2p_port");
        logger.info("Listening on {}:{}", address, port);
        this.self = new Host(InetAddress.getByName(address), Integer.parseInt(port));
        forwarder = new Host(InetAddress.getByName("localhost"),Integer.parseInt(props.getProperty("p2p_port_f")));

        channelId = createChannel("BabelNewTCPChannel.NAME", props);
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
        registerMessageHandler(channelId, EndOfStreaming.ID,this::uponEndOfStreamingMessage);

        registerChannelEventHandler(channelId, OnConnectionUpEvent.EVENT_ID, this::uponInConnectionDown);
        //openConnection(forwarder);
    }

    private void uponInConnectionDown(OnConnectionUpEvent event, int channelId) {
        try {
            System.out.println("CONNECTION CLOSED 2! "+totoal);
            fos.close();
        }catch (Exception e){e.printStackTrace();};
    }
    int totoal = 0;
    private void uponReceiveMessage(StreamMessage msg, Host from, short sourceProto, int channelId){
        try{
            totoal +=msg.getDataLength();
            fos.write(msg.getData(), 0,msg.getDataLength());
            fos.flush();
            //sendMessage(msg,forwarder);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void uponEndOfStreamingMessage(EndOfStreaming msg, Host from, short sourceProto, int channelId){
        logger.info("{} RECEIVED EOS FROM {}.",self,from);
        try {
            System.out.println("CONNECTION CLOSED! "+totoal);
            logger.info("{} Stream Ended! from {}",self,from);
            fos.close();
            //StreamMessage streamMessage = new StreamMessage(new byte[0],0,"OLA");
            //sendMessage(streamMessage,forwarder);
        }catch (Exception e){e.printStackTrace();};
    }
}
