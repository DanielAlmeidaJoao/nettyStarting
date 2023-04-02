package babel.appExamples.protocols.quicProtocols.echoQuicProtocol;

import babel.appExamples.channels.BabelStreamingChannel;
import babel.appExamples.channels.babelQuicChannel.BabelQuicChannel;
import babel.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

public class EchoProtocol extends GenericProtocol {


    private static final Logger logger = LogManager.getLogger(EchoProtocol.class);
    public static final short PROTOCOL_ID = 200;
    private int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public EchoProtocol(Properties properties) throws IOException {
        super(EchoProtocol.class.getName(),PROTOCOL_ID);
        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        logger.info("Receiver on {}:{}", address, port);
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));
        channelId = createChannel(BabelQuicChannel.NAME, properties);
        this.properties = properties;
    }

    @Override
    public void init(Properties props) {
        //Nothing to do here, we just wait for event from the membership or the application
        registerMessageSerializer(channelId, EchoMessage.MSG_ID, EchoMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            registerMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessage, this::uponMsgFail);
            if(myself.getPort()==8081){
                //Integer.parseInt(props.getProperty("nei_port")
                dest = new Host(InetAddress.getByName("localhost"),8082);
                openConnection(dest);
                registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
                registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
            }
        } catch (Exception e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest!=null){

            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
    }
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest!=null){

            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
    }
    private void uponFloodMessage(EchoMessage msg, Host from, short sourceProto, int channelId) {
        logger.trace("Received {} from {}", msg.getMessage(), from);
    }

    private void uponMsgFail(EchoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
}
