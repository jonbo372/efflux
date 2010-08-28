package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtcpPacket;
import org.factor45.efflux.packet.RtpPacket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A regular RTP session, as described in RFC3550.
 *
 * Unlike {@link SingleParticipantSession}, this session starts off with 0 remote participants.
 *
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class MultiParticipantSession extends AbstractRtpSession {

    // internal vars --------------------------------------------------------------------------------------------------

    private final Map<Long, RtpParticipantContext> participantTable;

    // constructors ---------------------------------------------------------------------------------------------------

    public MultiParticipantSession(String id, int payloadType, RtpParticipant localParticipant) {
        super(id, payloadType, localParticipant);

        this.participantTable = new ConcurrentHashMap<Long, RtpParticipantContext>();
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public Collection<RtpParticipant> getRemoteParticipants() {
        Collection<RtpParticipant> participants = new ArrayList<RtpParticipant>(this.participantTable.size());
        for (RtpParticipantContext context : this.participantTable.values()) {
            participants.add(context.getParticipant());
        }
        
        return participants;
    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected boolean internalSendData(RtpPacket packet) {
        try {
            for (RtpParticipantContext context : this.participantTable.values()) {
                this.writeToData(packet, context.getParticipant().getDataAddress());
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send {} to participants in session with id {}.", this.id);
            return false;
        }
    }

    @Override
    protected boolean internalSendControl(RtcpPacket packet) {
        try {
            for (RtpParticipantContext context : this.participantTable.values()) {
                this.writeToControl(packet, context.getParticipant().getDataAddress());
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send {} to participants in session with id {}.", this.id);
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

        RtpParticipantContext context = this.getOrCreateParticipant(origin, packet);

        if (context.getLastSequenceNumber() >= packet.getSequenceNumber() && this.discardOutOfOrder) {
            LOG.trace("Discarded out of order packet for {} (last SN was {}, packet SN was {}, session id: {}).",
                      context.getParticipant(), context.getLastSequenceNumber(), packet.getSequenceNumber(), this.id);
            return;
        }

        context.setLastSequenceNumber(packet.getSequenceNumber());

        if (!origin.equals(context.getParticipant().getDataAddress())) {
            context.getParticipant().updateRtpAddress(origin);
            LOG.debug("Updated RTP address for {} to {} (session id: {}).", context.getParticipant(), origin, this.id);
        }

        for (RtpSessionDataListener listener : this.dataListeners) {
            listener.dataPacketReceived(this, context.getParticipant(), packet);
        }
    }

    private RtpParticipantContext getOrCreateParticipant(SocketAddress origin, RtpPacket packet) {
        RtpParticipantContext context = this.getParticipantContext(packet.getSynchronisationSourceId());
        if (context == null) {
            // New participant
            RtpParticipant p = RtpParticipant.createFromUnexpectedDataPacket((InetSocketAddress) origin, packet);
            context = new RtpParticipantContext(p);
            this.saveParticipantContext(context);

            LOG.debug("New participant joined session with id {} (from data packet): {}.", this.id, p);
            for (RtpSessionEventListener listener : eventListeners) {
                listener.participantJoinedFromData(this, p, packet);
            }
        }

        return context;
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------

    @Override
    public void controlPacketReceived(SocketAddress origin, RtcpPacket packet) {

    }

    // private helpers ------------------------------------------------------------------------------------------------

    private void saveParticipantContext(RtpParticipantContext context) {
        this.participantTable.put(context.getParticipant().getSynchronisationSourceId(), context);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipantContext getParticipantContext(Long synchronisationSourceId) {
        return this.participantTable.get(synchronisationSourceId);
    }

    public Map<Long, RtpParticipantContext> getParticipantTable() {
        return Collections.unmodifiableMap(this.participantTable);
    }
}
