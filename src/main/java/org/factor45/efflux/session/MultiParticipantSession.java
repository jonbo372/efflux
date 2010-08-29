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
 * <div class="note">
 * <div class="header">Note:</div>
 * This class is not completely thread-safe when making changes to the participant table. It is theoretically possible
 * that a new participant is created at the exact same time both via API and events. Since the chances are so infimal
 * and the results have no real consequences (only real information loss would be the internal fields of the
 * {@link RtpParticipant}) I chose to keep it this way in order to avoid speed penalties when traversing the participant
 * list both for sending and receiving data due to high contention.
 * </div>
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
    public boolean addParticipant(RtpParticipant remoteParticipant) {
        if (remoteParticipant.getSsrc() == this.localParticipant.getSsrc()) {
            return false;
        }

        RtpParticipantContext context = this.participantTable.get(remoteParticipant.getSsrc());
        if (context == null) {
            context = new RtpParticipantContext(remoteParticipant);
            this.participantTable.put(remoteParticipant.getSsrc(), context);
            return true;
        }

        return false;
    }

    @Override
    public RtpParticipant removeParticipant(long ssrc) {
        RtpParticipantContext context = this.participantTable.remove(ssrc);
        if (context == null) {
            return null;
        }

        return context.getParticipant();
    }

    @Override
    public RtpParticipant getRemoteParticipant(long ssrc) {
        RtpParticipantContext context = this.participantTable.get(ssrc);
        if (context == null) {
            return null;
        }

        return context.getParticipant();
    }

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

    @Override
    protected RtpParticipantContext getContext(SocketAddress origin, RtpPacket packet) {
        // Get or create.
        RtpParticipantContext context = this.participantTable.get(packet.getSsrc());
        if (context == null) {
            // New participant
            RtpParticipant participant = RtpParticipant.createFromUnexpectedDataPacket((InetSocketAddress) origin, packet);
            context = new RtpParticipantContext(participant);
            this.participantTable.put(participant.getSsrc(), context);

            LOG.debug("New participant joined session with id {} (from data packet): {}.", this.id, participant);
            for (RtpSessionEventListener listener : this.eventListeners) {
                listener.participantJoinedFromData(this, participant, packet);
            }
        }

        return context;
    }

    @Override
    protected boolean doBeforeDataReceivedValidation(RtpPacket packet) {
        return true;
    }

    @Override
    protected boolean doAfterDataReceivedValidation(SocketAddress origin) {
        return true;
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------

    @Override
    public void controlPacketReceived(SocketAddress origin, RtcpPacket packet) {

    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipantContext getParticipantContext(long ssrc) {
        return this.participantTable.get(ssrc);
    }

    public Map<Long, RtpParticipantContext> getParticipantTable() {
        return Collections.unmodifiableMap(this.participantTable);
    }
}
