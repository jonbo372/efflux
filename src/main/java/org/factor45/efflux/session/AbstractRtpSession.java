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
import java.util.concurrent.atomic.AtomicBoolean;
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
    protected final AtomicBoolean sentOrReceivedPackets;

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
        this.sentOrReceivedPackets = new AtomicBoolean(false);

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

        LOG.debug("Data & Control channels bound for RtpSession with id {}.", this.id);
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
        LOG.debug("RtpSession with id {} terminated.", this.id);

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
        packet.setSsrc(this.localParticipant.getSsrc());
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

    // DataPacketReceiver ---------------------------------------------------------------------------------------------

    @Override
    public void dataPacketReceived(SocketAddress origin, RtpPacket packet) {
        if (packet.getPayloadType() != this.payloadType) {
            // Silently discard packets of wrong payload.
            return;
        }

        if (packet.getSsrc() == this.localParticipant.getSsrc()) {
            long oldSsrc = this.localParticipant.getSsrc();

            // A collision has been detected after packets were sent, resolve by updating the local SSRC and sending
            // a BYE RTCP packet for the old SSRC.
            // http://tools.ietf.org/html/rfc3550#section-8.2
            // If no packet was sent and this is the first being received then we can avoid collisions by switching
            // our own SSRC to something else (nothing else is required because the collision was prematurely detected
            // and avoided).
            // http://tools.ietf.org/html/rfc3550#section-8.1, last paragraph
            if (this.sentOrReceivedPackets.getAndSet(true)) {
                // TODO create and send RTCP BYE
            }

            LOG.warn("SSRC collision with remote end detected on session with id {}; updating SSRC from {} to {}.",
                     this.id, oldSsrc, this.localParticipant.resolveSsrcConflict(packet.getSsrc()));
            for (RtpSessionEventListener listener : this.eventListeners) {
                listener.resolvedSsrcConflict(this, oldSsrc, this.localParticipant.getSsrc());
            }
        }

        // Associate the packet with a participant or create one.
        RtpParticipantContext context = this.getContext(origin, packet);
        if (context == null) {
            // Implementations of this class SHOULD never return null here...
            return;
        }

        if (!this.doBeforeDataReceivedValidation(packet)) {
            // Subclass does not want to proceed due to some check failing.
            return;
        }

        // Should the packet be discarded due to out of order SN?
        if ((context.getLastSequenceNumber() >= packet.getSequenceNumber()) && this.discardOutOfOrder) {
            LOG.trace("Discarded out of order packet (last SN was {}, packet SN was {}).",
                      context.getLastSequenceNumber(), packet.getSequenceNumber());
            return;
        }

        // Update last SN and location for participant.
        // We trust the SSRC rather than the ip/port to identify participants (mostly because of NAT).
        context.setLastSequenceNumber(packet.getSequenceNumber());
        if (!origin.equals(context.getParticipant().getDataAddress())) {
            context.getParticipant().updateRtpAddress(origin);
            LOG.debug("Updated RTP address for {} to {} (session id: {}).", context.getParticipant(), origin, this.id);
        }

        if (!this.doAfterDataReceivedValidation(origin)) {
            // Subclass does not want to proceed due to some check failing.
            return;
        }

        // Finally, dispatch the event to the data listeners.
        for (RtpSessionDataListener listener : this.dataListeners) {
            listener.dataPacketReceived(this, context.getParticipant(), packet);
        }
    }

    // protected helpers ----------------------------------------------------------------------------------------------

    protected abstract boolean internalSendData(RtpPacket packet);

    protected abstract boolean internalSendControl(RtcpPacket packet);

    protected abstract RtpParticipantContext getContext(SocketAddress origin, RtpPacket packet);

    protected abstract boolean doBeforeDataReceivedValidation(RtpPacket packet);

    protected abstract boolean doAfterDataReceivedValidation(SocketAddress origin);

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
