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
        if("8081".equals(port)){
            InetSocketAddress remote = new InetSocketAddress("localhost",8082);
            testQuicChannel.openConnection(remote);
        }
        System.out.println("CONNECTED");
        Scanner scanner = new Scanner(System.in);


        String input = "";

        while (!"quit".equalsIgnoreCase(input)){
            System.out.println("Enter data:");
            input = scanner.nextLine();
            System.out.println("Enter streamId:");
            String streamId = scanner.nextLine().trim();
            System.out.println("ENTERED: "+input);
            System.out.println("STREAM_ID: "+streamId);
            testQuicChannel.send(streamId,input.getBytes(),input.length());
        }
        System.out.println("STILL HERE!!!");
        scanner.close();
    }
}