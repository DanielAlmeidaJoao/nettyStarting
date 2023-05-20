package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BabelQuicChannelWithControlledClose<T> extends BabelQuicChannel<T> {
    private static final Logger logger = LogManager.getLogger(BabelQuicChannelWithControlledClose.class);
    private Map<Host, Set<Short>> hostSetMap;
    private Set<Short> registeredProtos;
    public BabelQuicChannelWithControlledClose(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(serializer,list,properties);
        hostSetMap = new ConcurrentHashMap<>();
        registeredProtos = new ConcurrentSkipListSet<>();
    }
    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        Set<Short> protosUsingThisConnection = hostSetMap.get(proto);
        if(protosUsingThisConnection!=null){
            protosUsingThisConnection.add(proto);
        }
        super.sendMessage(msg,peer,proto);
    }
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer){
        hostSetMap.put(FactoryMethods.toBabelHost(peer),new HashSet<>(registeredProtos));
        super.onConnectionUp(incoming,peer);
    }
    @Override
    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
        hostSetMap.remove(FactoryMethods.toBabelHost(peer));
        super.onConnectionDown(peer,incoming);
    }
    @Override
    public void closeConnection(Host peer, short proto) {
        Set<Short> protosUsingThisConnection = hostSetMap.get(peer);
        if (protosUsingThisConnection!=null){
            if(protosUsingThisConnection.remove(proto) && protosUsingThisConnection.isEmpty()){
                super.closeConnection(peer,proto);
                hostSetMap.remove(proto);
            }
        }else{
            super.closeConnection(peer,proto);
        }
    }
    @Override
    public void registerChannelInterest(short protoId) {
        registeredProtos.add(protoId);
    }
}
