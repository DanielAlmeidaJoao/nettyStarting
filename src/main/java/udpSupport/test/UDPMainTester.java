package udpSupport.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Properties;

public class UDPMainTester {
    private static final Logger logger = LogManager.getLogger(UDPMainTester.class);

    public static void main(String [] args) throws Exception {
        logger.info("STARREDD");
        Properties properties = new Properties();
        properties.setProperty("UDP_address","localhost");
        properties.setProperty("UDP_metrics","on");
        String port = args[0];
        properties.setProperty("UDP_port",port);
        TestUDPChannel testUDPChannel = new TestUDPChannel(properties);
        if(port.equals("8081")){
            InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost",8082);
            testUDPChannel.startStreaming(inetSocketAddress);
        }
    }
}
