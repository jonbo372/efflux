package org.factor45.efflux;

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
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Implementation that only supports two participants, a local and the remote.
 *
 * This session is ideal for calls with only two participants in NAT scenarions, where often the IP and ports
 * negociated in the SDP aren't the real ones (due to NAT restrictions and clients not supporting ICE).
 *
 * If data is received from a source other than the expected one, this session will automatically update the
 * destination IP and newly sent packets will be addressed to that new IP rather than the old one.
 *
 * If more than one source is used to send data for this session it will often get "confused" and keep redirecting
 * packets to the last source from which it received.
 *
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class SingleParticipantSession implements RtpSession {

    // constants ------------------------------------------------------------------------------------------------------

    protected static final InternalLogger LOG = InternalLoggerFactory.getInstance(SingleParticipantSession.class);

    // configuration defaults -----------------------------------------------------------------------------------------

    private static final boolean USE_NIO = false;

    // configuration --------------------------------------------------------------------------------------------------

    private final String id;
    private final Participant remoteParticipant;
    private String host;
    private final int dataPort;
    private final int controlPort;

    // internal vars --------------------------------------------------------------------------------------------------

    private final List<RtpSessionListener> listeners;
    private boolean useNio;
    private boolean initialised;
    private ConnectionlessBootstrap dataBootstrap;
    private ConnectionlessBootstrap controlBootstrap;
    private DatagramChannel dataChannel;
    private DatagramChannel controlChannel;

    // constructors ---------------------------------------------------------------------------------------------------

    public SingleParticipantSession(String id, int dataPort, int controlPort, Participant remoteParticipant) {
        if ((dataPort < 0) || (dataPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        if ((controlPort < 0) || (controlPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        this.id = id;
        this.dataPort = dataPort;
        this.controlPort = controlPort;
        this.remoteParticipant = remoteParticipant;
        this.useNio = USE_NIO;
        this.listeners = new CopyOnWriteArrayList<RtpSessionListener>();
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public String getId() {
        return id;
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
        this.dataBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new DataPacketDecoder(),
                                         DataPacketEncoder.getInstance(),
                                         new DataHandler(SingleParticipantSession.this));
            }
        });
        this.controlBootstrap = new ConnectionlessBootstrap(factory);
        this.controlBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new ControlPacketDecoder(),
                                         ControlPacketEncoder.getInstance(),
                                         new ControlHandler(SingleParticipantSession.this));
            }
        });

        SocketAddress dataAddress;
        SocketAddress controlAddress;
        if (this.host == null) {
            dataAddress = new InetSocketAddress(this.dataPort);
            controlAddress = new InetSocketAddress(this.controlPort);
        } else {
            dataAddress = new InetSocketAddress(this.host, this.dataPort);
            controlAddress = new InetSocketAddress(this.host, this.controlPort);
        }

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
    public boolean sendDataPacket(RtpPacket packet) {
        if (!this.initialised) {
            return false;
        }

        this.dataChannel.write(packet, this.remoteParticipant.getRtpAddress());
        return true;
    }

    @Override
    public boolean sendControlPacket(RtcpPacket packet) {
        if (!this.initialised) {
            return false;
        }

        this.controlChannel.write(packet, this.remoteParticipant.getRtcpAddress());
        return true;
    }

    @Override
    public void addListener(RtpSessionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(RtpSessionListener listener) {
        this.listeners.remove(listener);
    }

    // DataPacketReceiver ---------------------------------------------------------------------------------------------

    @Override
    public void dataPacketReceived(RtpPacket packet, SocketAddress origin) {
        if (!origin.equals(this.remoteParticipant.getRtpAddress())) {
            this.remoteParticipant.updateRtpAddress(origin);
            LOG.debug("Updated remote participant's RTP address to " + origin + " in RtpSession with id " + this.id);
        }

        for (RtpSessionListener listener : listeners) {
            listener.dataPacketReceived(this, packet);
        }
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------

    @Override
    public void controlPacketReceived(RtcpPacket packet, SocketAddress origin) {
        System.err.println("Received control " + packet + " from " + origin);
        if (origin != this.remoteParticipant.getRtcpAddress()) {
            this.remoteParticipant.updateRtcpAddress(origin);
            LOG.debug("Updated remote participant's RTCP address to " + origin + " in RtpSession with id " + this.id);
        }
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public Participant getRemoteParticipant() {
        return remoteParticipant;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (this.initialised) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.host = host;
    }

    public int getDataPort() {
        return dataPort;
    }

    public int getControlPort() {
        return controlPort;
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
}
