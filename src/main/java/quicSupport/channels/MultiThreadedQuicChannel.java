package quicSupport.channels;

import quicSupport.utils.NetworkRole;

import java.io.IOException;
import java.util.Properties;

public abstract class MultiThreadedQuicChannel extends CustomQuicChannel {
    public MultiThreadedQuicChannel(Properties properties, NetworkRole role) throws IOException {
        super(properties,false,role);
    }
}
