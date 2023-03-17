import babel.appExamples.channels.BabelStreamingChannel;
import babel.appExamples.channels.initializers.BabelStreamInitializer;
import babel.appExamples.protocols.ReceiveFileProtocol;
import babel.appExamples.protocols.SendFileProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;
import quicSupport.testing.TestQuicChannel;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Scanner;

public class Main2 {

    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }
    private static final Logger logger = LogManager.getLogger(TestQuicChannel.class);

    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        logger.info("STARREDD");
        Properties properties = new Properties();
        properties.setProperty("address","localhost");
        String port = args[0];
        properties.setProperty("port",port);
        TestQuicChannel testQuicChannel = new TestQuicChannel(properties);
        /**
        if("8081".equals(port)){
            InetSocketAddress remote = new InetSocketAddress("localhost",8082);
            testQuicChannel.openConnection(remote);
        }
        System.out.println("CONNECTED"); **/
        Scanner scanner = new Scanner(System.in);


        String input = "";

        while (!"quit".equalsIgnoreCase(input)){
            input = scanner.nextLine();
            if(input.equalsIgnoreCase("S")){//send message
                System.out.println("Enter data:");
                input = scanner.nextLine();
                System.out.println("Enter streamId:");
                String streamId = scanner.nextLine().trim();
                System.out.println("ENTERED: "+input);
                System.out.println("STREAM_ID: "+streamId);
                testQuicChannel.send(streamId,input.getBytes(),input.length());
            }else if(input.equalsIgnoreCase("C")){//create stream
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.createStream(address);
            }else if(input.equalsIgnoreCase("CS")){//close stream
                System.out.println("STREAM ID:");
                String streamId = scanner.nextLine();
                testQuicChannel.closeStream(streamId);
            }else if(input.equalsIgnoreCase("O")){//OPEN CONNECTION
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.openConnection(address);
            }else if(input.equalsIgnoreCase("CC")){//CLOSE CONNECTIONS
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.closeConnection(address);
            }

        }
        System.out.println("STILL HERE!!!");
        scanner.close();
    }
}