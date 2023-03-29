package quicSupport.channels;

import java.io.IOException;
import java.util.Properties;

public abstract class MultiThreadedQuicChannel extends CustomQuicChannel {
    public MultiThreadedQuicChannel(Properties properties) throws IOException {
        super(properties);
    }
}
