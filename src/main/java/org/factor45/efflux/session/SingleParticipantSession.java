package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtcpPacket;
import org.factor45.efflux.packet.RtpPacket;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;

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
public class SingleParticipantSession extends AbstractRtpSession {

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipantContext context;

    // constructors ---------------------------------------------------------------------------------------------------

    public SingleParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                    RtpParticipant remoteParticipant) {
        super(id, payloadType, localParticipant);
        this.context = new RtpParticipantContext(remoteParticipant);
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public Collection<RtpParticipant> getRemoteParticipants() {
        return Arrays.asList(this.context.getParticipant());
    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected boolean internalSendData(RtpPacket packet) {
        try {
            this.writeToData(packet, this.context.getParticipant().getDataAddress());
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send {} to {} in session with id {}.", this.id, this.context.getParticipant());
            return false;
        }
    }

    @Override
    protected boolean internalSendControl(RtcpPacket packet) {
        try {
            this.writeToControl(packet, this.context.getParticipant().getControlAddress());
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send {} to {} in session with id {}.", this.id, this.context.getParticipant());
            return false;
        }
    }

    // DataPacketReceiver ---------------------------------------------------------------------------------------------

    @Override
    public void dataPacketReceived(SocketAddress origin, RtpPacket packet) {
        if (packet.getPayloadType() != this.payloadType) {
            // Silently discard packets of wrong payload
            return;
        }

        if (this.context.getLastSequenceNumber() >= packet.getSequenceNumber() && this.discardOutOfOrder) {
            LOG.trace("Discarded out of order packet (last SN was {}, packet SN was {}).",
                      this.context.getLastSequenceNumber(), packet.getSequenceNumber());
            return;
        }

        this.context.setLastSequenceNumber(packet.getSequenceNumber());

        if (!origin.equals(this.context.getParticipant().getDataAddress())) {
            this.context.getParticipant().updateRtpAddress(origin);
            LOG.debug("Updated remote participant's RTP address to {} in RtpSession with id {}.", origin, this.id);
        }

        for (RtpSessionDataListener listener : this.dataListeners) {
            listener.dataPacketReceived(this, this.context.getParticipant(), packet);
        }
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------

    @Override
    public void controlPacketReceived(SocketAddress origin, RtcpPacket packet) {
        System.err.println("Received control " + packet + " from " + origin);
        if (origin != this.context.getParticipant().getControlAddress()) {
            this.context.getParticipant().updateRtcpAddress(origin);
            LOG.debug("Updated remote participant's RTCP address to {} in RtpSession with id {}.", origin, this.id);
        }
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipant getRemoteParticipant() {
        return this.context.getParticipant();
    }
}
