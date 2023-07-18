package udpSupport.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Properties;

public class UDPMainTester {
    private static final Logger logger = LogManager.getLogger(UDPMainTester.class);

    public static void main(String [] args) throws Exception {
        logger.info("STARREDD");
        String selfHost = args[0];
        String port = args[1];
        System.out.println("SELF HOST "+selfHost);
        Properties properties = new Properties();
        properties.setProperty("UDP_address",selfHost);
        properties.setProperty("UDP_metrics","on");
        properties.setProperty("UDP_port",port);
        properties.setProperty(udpSupport.client_server.NettyUDPServer.UDP_RETRANSMISSION_TIMEOUT,"1");
        properties.setProperty(udpSupport.client_server.NettyUDPServer.MAX_SEND_RETRIES_KEY,"100");
        TestUDPChannel testUDPChannel = new TestUDPChannel(properties);
        //testUDPChannel.interact();
        if(port.equals("8081")){
            InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost",8082);
            testUDPChannel.startStreaming(inetSocketAddress);
        }
    }
}
