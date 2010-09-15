/*
 * Copyright 2010 Bruno de Carvalho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.factor45.efflux.session;

import org.factor45.efflux.packet.CompoundControlPacket;
import org.factor45.efflux.packet.ControlPacket;
import org.factor45.efflux.packet.DataPacket;
import org.factor45.efflux.participant.ParticipantDatabase;
import org.factor45.efflux.participant.RtpParticipant;
import org.factor45.efflux.participant.RtpParticipantInfo;
import org.factor45.efflux.participant.SingleParticipantDatabase;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation that only supports two participants, a local and the remote.
 * <p/>
 * This session is ideal for calls with only two participants in NAT scenarions, where often the IP and ports negociated
 * in the SDP aren't the real ones (due to NAT restrictions and clients not supporting ICE).
 * <p/>
 * If data is received from a source other than the expected one, this session will automatically update the destination
 * IP and newly sent packets will be addressed to that new IP rather than the old one.
 * <p/>
 * If more than one source is used to send data for this session it will often get "confused" and keep redirecting
 * packets to the last source from which it received.
 * <p>
 * This is <strong>NOT</strong> a fully RFC 3550 compliant implementation, but rather a special purpose one for very
 * specific scenarios.
 *
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public class SingleParticipantSession extends AbstractRtpSession {

    // configuration defaults -----------------------------------------------------------------------------------------

    private static final boolean IGNORE_FROM_UNKNOWN_SSRC = true;

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipant receiver;
    private boolean ignoreFromUnknownSsrc;

    // internal vars --------------------------------------------------------------------------------------------------

    private final AtomicBoolean receivedPackets;

    // constructors ---------------------------------------------------------------------------------------------------

    public SingleParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                    RtpParticipant remoteParticipant) {
        super(id, payloadType, localParticipant);
        if (!remoteParticipant.isReceiver()) {
            throw new IllegalArgumentException("Remote participant must be a receiver (data & control addresses set)");
        }
        this.receiver = remoteParticipant;
        this.receivedPackets = new AtomicBoolean(false);
        this.ignoreFromUnknownSsrc = IGNORE_FROM_UNKNOWN_SSRC;
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public boolean addReceiver(RtpParticipant remoteParticipant) {
        if (this.receiver.equals(remoteParticipant)) {
            return true;
        }

        // Sorry, "there can be only one".
        return false;
    }

    @Override
    public boolean removeReceiver(RtpParticipant remoteParticipant) {
        // No can do.
        return false;
    }

    @Override
    public RtpParticipant getRemoteParticipant(long ssrc) {
        if (ssrc == this.receiver.getInfo().getSsrc()) {
            return this.receiver;
        }

        return null;
    }

    @Override
    public Map<Long, RtpParticipant> getRemoteParticipants() {
        Map<Long, RtpParticipant> map = new HashMap<Long, RtpParticipant>();
        map.put(this.receiver.getSsrc(), this.receiver);
        return map;
    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected ParticipantDatabase createDatabase() {
        return new SingleParticipantDatabase(this.id, this.receiver);
    }

    @Override
    protected void internalSendData(DataPacket packet) {
        try {
            this.writeToData(packet, this.receiver.getDataDestination());
            this.sentOrReceivedPackets.set(true);
        } catch (Exception e) {
            LOG.error("Failed to send {} to {} in session with id {}.", this.id, this.receiver.getInfo());
        }
    }

    @Override
    protected void internalSendControl(ControlPacket packet) {
        try {
            this.writeToControl(packet, this.receiver.getControlDestination());
            this.sentOrReceivedPackets.set(true);
        } catch (Exception e) {
            LOG.error("Failed to send RTCP packet to {} in session with id {}.",
                      this.receiver.getInfo(), this.id);
        }
    }

    @Override
    protected void internalSendControl(CompoundControlPacket packet) {
        try {
            this.writeToControl(packet, this.receiver.getControlDestination());
            this.sentOrReceivedPackets.set(true);
        } catch (Exception e) {
            LOG.error("Failed to send compound RTCP packet to {} in session with id {}.",
                      this.receiver.getInfo(), this.id);
        }
    }

    // DataPacketReceiver ---------------------------------------------------------------------------------------------

    @Override
    public void dataPacketReceived(SocketAddress origin, DataPacket packet) {
        if (!this.receivedPackets.getAndSet(true)) {
            // If this is the first packet then setup the SSRC for this participant (we didn't know it yet).
            this.receiver.getInfo().setSsrc(packet.getSsrc());
            LOG.trace("First packet received from remote source, updated SSRC to {}.", packet.getSsrc());
        } else if (this.ignoreFromUnknownSsrc && (packet.getSsrc() != this.receiver.getInfo().getSsrc())) {
            LOG.trace("Discarded packet from unexpected SSRC: {} (expected was {}).",
                      packet.getSsrc(), this.receiver.getInfo().getSsrc());
            return;
        }

        super.dataPacketReceived(origin, packet);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipantInfo getRemoteParticipant() {
        return this.receiver.getInfo();
    }

    public boolean isIgnoreFromUnknownSsrc() {
        return ignoreFromUnknownSsrc;
    }

    public void setIgnoreFromUnknownSsrc(boolean ignoreFromUnknownSsrc) {
        this.ignoreFromUnknownSsrc = ignoreFromUnknownSsrc;
    }
}
