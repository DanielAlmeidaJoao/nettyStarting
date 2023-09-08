package quicSupport.utils.customConnections;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.quic.QuicChannel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetricsManager;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class CustomQUICConnection {
    private static final Logger logger = LogManager.getLogger(CustomQUICConnection.class);
    private final QuicChannel connection;
    private CustomQUICStreamCon defaultStream;
    private final boolean  inComing;
    private Map<String, CustomQUICStreamCon> nettyIdToCustomStreamCon;
    private ScheduledFuture scheduledFuture;
    public TransmissionType transmissionType;

    private InetSocketAddress remote;
    private boolean canSendHeartBeat;
    private static float heartBeatTimeout;
    private final long creationTime;
    public final ConnectionProtocolMetricsManager metricsManager;

    float calcHeartBeatTimeoutPercentage(int heartBeatTimeout, int percentage){
        if(percentage <= 0) return 0;
        float x = percentage;
        float y = heartBeatTimeout;
        int res = (int) ((x/100)*y);
        return (res >= heartBeatTimeout) ? (int)0.5*heartBeatTimeout : res;
    }
    public CustomQUICConnection(CustomQUICStreamCon quicStreamChannel, InetSocketAddress remote, boolean inComing, int idleTimeoutPercentageHB, int hbTimeout, TransmissionType type, ConnectionProtocolMetricsManager metrics){
        creationTime = System.currentTimeMillis();
        defaultStream = quicStreamChannel;
        connection = defaultStream.streamChannel.parent();
        this.inComing = inComing;
        nettyIdToCustomStreamCon = new HashMap<>();
        this.remote = remote;
        addStream(defaultStream);
        scheduledFuture = null;
        canSendHeartBeat = inComing;
        this.heartBeatTimeout = calcHeartBeatTimeoutPercentage(hbTimeout,idleTimeoutPercentageHB);
        transmissionType = type;
        this.metricsManager = metrics;
        if(inComing&&this.heartBeatTimeout>0){
            logger.info("HB ENABLED !!!");
            serverStartScheduling();
        }
        //logger.info("CONNECTION TO {} ON. DEFAULT STREAM: {} .",remote,defaultStream.id().asShortText());
    }
    public boolean hasPassedOneSec(){
        return (System.currentTimeMillis()-creationTime)>2000;
    }
    public void addStream(CustomQUICStreamCon streamChannel){
        nettyIdToCustomStreamCon.put(streamChannel.streamChannel.id().asShortText(),streamChannel);
    }
    public void closeStream(String nettyId) {
        CustomQUICStreamCon streamChannel = nettyIdToCustomStreamCon.remove(nettyId);

        if(nettyIdToCustomStreamCon.isEmpty()){
            connection.disconnect();
            connection.close();
        }else if(streamChannel==defaultStream){
            defaultStream = nettyIdToCustomStreamCon.entrySet().iterator().next().getValue();
            transmissionType = defaultStream.type;
        }

        //streamChannel.streamChannel.shutdown();
        //streamChannel.streamChannel.disconnect();
    }
    public CustomQUICStreamCon getMessageStream(){
        if(TransmissionType.STRUCTURED_MESSAGE == defaultStream.type){
            return defaultStream;
        }
        for (CustomQUICStreamCon value : nettyIdToCustomStreamCon.values()) {
            if(TransmissionType.STRUCTURED_MESSAGE==value.type){
                return value;
            }
        }
        return null;
    }
    public void close(){
        //streams = null;
        connection.disconnect();
        connection.close();
    }
    public void closeAll(){
        for (CustomQUICStreamCon value : nettyIdToCustomStreamCon.values()) {
            value.close();
        }
    }

    private void serverStartScheduling(){
        if(inComing){
            scheduleSendHeartBeat_KeepAlive();
            canSendHeartBeat=false;
        }
    }

    public void scheduleSendHeartBeat_KeepAlive(){
            final CustomQUICStreamCon stream = getMessageStream();
            if(scheduledFuture!=null){
                scheduledFuture.cancel(true);
            }
            //|| TransmissionType.UNSTRUCTURED_STREAM == stream.type
            if(stream==null)return;
            scheduledFuture = stream.streamChannel.eventLoop().schedule(() -> {
                ByteBuf byteBuf = stream.streamChannel.alloc().directBuffer(4+1);
                byteBuf.writeInt(1); //msg length
                byteBuf.writeByte(QUICLogics.KEEP_ALIVE); //msg code
                byteBuf.writeByte(QUICLogics.KEEP_ALIVE); // the message
                stream.streamChannel.writeAndFlush(byteBuf);
                if(metricsManager != null){
                    metricsManager.calcControlMetricsOnSend(true,defaultStream.customStreamId,2);
                }
            }, (long)heartBeatTimeout,TimeUnit.SECONDS);
    }
    public boolean connectionDown(){
        return connection.isTimedOut();
    }
}
