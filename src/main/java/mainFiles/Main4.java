package mainFiles;

import appExamples2.appExamples.channels.babelQuicChannel.BabelQuicChannel;
import appExamples2.appExamples.channels.babelQuicChannel.BabelQuicInitializer;
import appExamples2.appExamples.channels.initializers.BabelStreamInitializer;
import appExamples2.appExamples.channels.newTCPChannel.BabelNewTCPChannel;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.StreamFileWithQUIC;
import pt.unl.fct.di.novasys.babel.core.Babel;

import java.util.Properties;

public class Main4 {
    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }
    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world 3!");

        //Get the (singleton) pt.unl.fct.di.novasys.babel instance
        Babel babel = Babel.getInstance();
        babel.registerChannelInitializer(BabelQuicChannel.NAME,new BabelQuicInitializer());
        babel.registerChannelInitializer(BabelNewTCPChannel.NAME,new BabelStreamInitializer());


        //Loads properties from the configuration file, and merges them with
        // properties passed in the launch argumentsSD=300
        Properties props = Babel.loadConfig(args, DEFAULT_CONF);

        //If you pass an interface name in the properties (either file or arguments), this wil get the
        // IP of that interface and create a property "address=ip" to be used later by the channels.
        StreamFileWithQUIC streamFileProtocol = new StreamFileWithQUIC(props);
        //EchoProtocol2 echoProtocol2 = new EchoProtocol2(props);
        babel.registerProtocol(streamFileProtocol);
        //babel.registerProtocol(echoProtocol2);
        streamFileProtocol.init(props);
        //echoProtocol2.addChan(echoProtocol.channelId);

        //Start pt.unl.fct.di.novasys.babel and protocol threads
        babel.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("APP ENEDED!!");
        }));
        System.out.println("APP STARTED!!");
    }

}
