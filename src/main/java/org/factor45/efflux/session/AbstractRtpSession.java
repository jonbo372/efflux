package org.factor45.efflux.session;

import org.factor45.efflux.logging.Logger;
import org.factor45.efflux.network.ControlHandler;
import org.factor45.efflux.network.ControlPacketDecoder;
import org.factor45.efflux.network.ControlPacketEncoder;
import org.factor45.efflux.network.DataHandler;
import org.factor45.efflux.network.DataPacketDecoder;
import org.factor45.efflux.network.DataPacketEncoder;
import org.factor45.efflux.packet.RtcpPacket;
import org.factor45.efflux.packet.RtpPacket;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public abstract class AbstractRtpSession implements RtpSession {

    // constants ------------------------------------------------------------------------------------------------------

    protected static final Logger LOG = Logger.getLogger(SingleParticipantSession.class);

    // configuration defaults -----------------------------------------------------------------------------------------

    protected static final boolean USE_NIO = false;
    protected static final boolean DISCARD_OUT_OF_ORDER = true;
    protected static final int SEND_BUFFER_SIZE = 1500;
    protected static final int RECEIVE_BUFFER_SIZE = 1500;

    // configuration --------------------------------------------------------------------------------------------------

    protected final String id;
    protected final int payloadType;
    protected String host;
    protected boolean discardOutOfOrder;
    protected int sendBufferSize;
    protected int receiveBufferSize;

    // internal vars --------------------------------------------------------------------------------------------------

    protected final RtpParticipant localParticipant;
    protected final List<RtpSessionDataListener> dataListeners;
    protected final List<RtpSessionEventListener> eventListeners;
    protected boolean useNio;
    protected boolean initialised;
    protected ConnectionlessBootstrap dataBootstrap;
    protected ConnectionlessBootstrap controlBootstrap;
    protected DatagramChannel dataChannel;
    protected DatagramChannel controlChannel;
    protected final AtomicInteger sequence;

    // constructors ---------------------------------------------------------------------------------------------------

    public AbstractRtpSession(String id, int payloadType, RtpParticipant local) {
        if ((payloadType < 0) || (payloadType > 127)) {
            throw new IllegalArgumentException("PayloadType must be in range [0;127]");
        }

        this.id = id;
        this.payloadType = payloadType;
        this.localParticipant = local;

        this.dataListeners = new CopyOnWriteArrayList<RtpSessionDataListener>();
        this.eventListeners = new CopyOnWriteArrayList<RtpSessionEventListener>();
        this.sequence = new AtomicInteger(0);

        this.useNio = USE_NIO;
        this.discardOutOfOrder = DISCARD_OUT_OF_ORDER;
        this.sendBufferSize = SEND_BUFFER_SIZE;
        this.receiveBufferSize = RECEIVE_BUFFER_SIZE;
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getPayloadType() {
        return this.payloadType;
    }

    @Override
    public synchronized boolean init() {
        DatagramChannelFactory factory;
        if (this.useNio) {
            factory = new OioDatagramChannelFactory(Executors.newCachedThreadPool());
        } else {
            factory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
        }

        this.dataBootstrap = new ConnectionlessBootstrap(factory);
        this.dataBootstrap.setOption("sendBufferSize", this.sendBufferSize);
        this.dataBootstrap.setOption("receiveBufferSize", this.receiveBufferSize);
        this.dataBootstrap.setOption("receiveBufferSizePredictorFactory",
                                     new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize));
        this.dataBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new DataPacketDecoder(),
                                         DataPacketEncoder.getInstance(),
                                         new DataHandler(AbstractRtpSession.this));
            }
        });
        this.controlBootstrap = new ConnectionlessBootstrap(factory);
        this.controlBootstrap.setOption("sendBufferSize", this.sendBufferSize);
        this.controlBootstrap.setOption("receiveBufferSize", this.receiveBufferSize);
        this.controlBootstrap.setOption("receiveBufferSizePredictorFactory",
                                        new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize));
        this.controlBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new ControlPacketDecoder(),
                                         ControlPacketEncoder.getInstance(),
                                         new ControlHandler(AbstractRtpSession.this));
            }
        });

        SocketAddress dataAddress = this.localParticipant.getDataAddress();
        SocketAddress controlAddress = this.localParticipant.getControlAddress();

        try {
            this.dataChannel = (DatagramChannel) this.dataBootstrap.bind(dataAddress);
        } catch (Exception e) {
            LOG.error("Failed to bind data channel for session with id " + this.id, e);
            this.dataBootstrap.releaseExternalResources();
            this.controlBootstrap.releaseExternalResources();
            return false;
        }
        try {
            this.controlChannel = (DatagramChannel) this.controlBootstrap.bind(controlAddress);
        } catch (Exception e) {
            LOG.error("Failed to bind control channel for session with id " + this.id, e);
            this.dataChannel.close();
            this.dataBootstrap.releaseExternalResources();
            this.controlBootstrap.releaseExternalResources();
            return false;
        }

        LOG.debug("Data & Control channels bound for RtpSession with id " + this.id);
        return (this.initialised = true);
    }

    @Override
    public synchronized void terminate() {
        if (!this.initialised) {
            return;
        }

        this.dataChannel.close();
        this.controlChannel.close();
        this.dataBootstrap.releaseExternalResources();
        this.controlBootstrap.releaseExternalResources();

        this.initialised = false;
    }

    @Override
    public boolean sendData(byte[] data, long timestamp) {
        if (!this.initialised) {
            return false;
        }

        RtpPacket packet = new RtpPacket();
        // Other fields will be set by sendDataPacket()
        packet.setTimestamp(timestamp);
        packet.setData(data);

        return this.sendDataPacket(packet);
    }

    @Override
    public boolean sendDataPacket(RtpPacket packet) {
        if (!this.initialised) {
            return false;
        }

        packet.setPayloadType(this.payloadType);
        packet.setSynchronisationSourceId(this.localParticipant.getSynchronisationSourceId());
        packet.setSequenceNumber(this.sequence.incrementAndGet());
        return this.internalSendData(packet);
    }

    @Override
    public boolean sendControlPacket(RtcpPacket packet) {
        return this.initialised && this.internalSendControl(packet);
    }

    @Override
    public RtpParticipant getLocalParticipant() {
        return this.localParticipant;
    }

    @Override
    public void addDataListener(RtpSessionDataListener listener) {
        this.dataListeners.add(listener);
    }

    @Override
    public void removeDataListener(RtpSessionDataListener listener) {
        this.dataListeners.remove(listener);
    }

    @Override
    public void addEventListener(RtpSessionEventListener listener) {
        this.eventListeners.add(listener);
    }

    @Override
    public void removeEventListener(RtpSessionEventListener listener) {
        this.eventListeners.remove(listener);
    }

    // protected helpers ----------------------------------------------------------------------------------------------

    protected abstract boolean internalSendData(RtpPacket packet);

    protected abstract boolean internalSendControl(RtcpPacket packet);

    protected void writeToData(RtpPacket packet, SocketAddress destination) {
        this.dataChannel.write(packet, destination);
    }

    protected void writeToControl(RtcpPacket packet, SocketAddress destination) {
        this.controlChannel.write(packet, destination);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.host = host;
    }

    public boolean useNio() {
        return useNio;
    }

    public void setUseNio(boolean useNio) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.useNio = useNio;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public boolean isDiscardOutOfOrder() {
        return discardOutOfOrder;
    }

    public void setDiscardOutOfOrder(boolean discardOutOfOrder) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.discardOutOfOrder = discardOutOfOrder;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.receiveBufferSize = receiveBufferSize;
    }
}
