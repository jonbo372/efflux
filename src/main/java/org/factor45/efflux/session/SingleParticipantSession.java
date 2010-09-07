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

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
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

    private final RtpParticipantContext context;
    private boolean ignoreFromUnknownSsrc;

    // internal vars --------------------------------------------------------------------------------------------------

    private final AtomicBoolean receivedPackets;

    // constructors ---------------------------------------------------------------------------------------------------

    public SingleParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                    RtpParticipant remoteParticipant) {
        super(id, payloadType, localParticipant);
        this.context = new RtpParticipantContext(remoteParticipant);
        this.receivedPackets = new AtomicBoolean(false);
        this.ignoreFromUnknownSsrc = IGNORE_FROM_UNKNOWN_SSRC;
    }

    // RtpSession -----------------------------------------------------------------------------------------------------

    @Override
    public boolean addParticipant(RtpParticipant remoteParticipant) {
        // Sorry, "there can be only one".
        return false;
    }

    @Override
    public RtpParticipant removeParticipant(long ssrc) {
        // No can do.
        return null;
    }

    @Override
    public RtpParticipant getRemoteParticipant(long ssrc) {
        if (ssrc == this.context.getParticipant().getSsrc()) {
            return this.context.getParticipant();
        }

        return null;
    }

    @Override
    public Collection<RtpParticipant> getRemoteParticipants() {
        return Arrays.asList(this.context.getParticipant());
    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected boolean internalSendData(DataPacket packet) {
        try {
            this.writeToData(packet, this.context.getParticipant().getDataAddress());
            this.sentOrReceivedPackets.set(true);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send {} to {} in session with id {}.", this.id, this.context.getParticipant());
            return false;
        }
    }

    @Override
    protected boolean internalSendControl(ControlPacket packet) {
        try {
            this.writeToControl(packet, this.context.getParticipant().getControlAddress());
            this.sentOrReceivedPackets.set(true);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send RTCP packet to {} in session with id {}.",
                      this.context.getParticipant(), this.id);
            return false;
        }
    }

    @Override
    protected boolean internalSendControl(CompoundControlPacket packet) {
        try {
            this.writeToControl(packet, this.context.getParticipant().getControlAddress());
            this.sentOrReceivedPackets.set(true);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send compound RTCP packet to {} in session with id {}.",
                      this.context.getParticipant(), this.id);
            return false;
        }
    }

    // DataPacketReceiver ---------------------------------------------------------------------------------------------

    @Override
    protected RtpParticipantContext getContext(SocketAddress origin, DataPacket packet) {
        return this.context;
    }

    @Override
    protected boolean doBeforeDataReceivedValidation(DataPacket packet) {
        if (!this.receivedPackets.getAndSet(true)) {
            // If this is the first packet then setup the SSRC for this participant (we didn't know it yet).
            this.context.getParticipant().updateSsrc(packet.getSsrc());
            LOG.trace("First packet received from remote source, updated SSRC to {}.", packet.getSsrc());
        } else if (this.ignoreFromUnknownSsrc && (packet.getSsrc() != this.context.getParticipant().getSsrc())) {
            LOG.trace("Discarded packet from unexpected SSRC: {} (expected was {}).",
                      packet.getSsrc(), this.context.getParticipant().getSsrc());
            return false;
        }

        // From here on we know that either the packet came from the expected SSRC or that we don't care about SSRC.

        return true;
    }

    @Override
    protected boolean doAfterDataReceivedValidation(SocketAddress origin) {
        return true;
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------

    @Override
    public void controlPacketReceived(SocketAddress origin, CompoundControlPacket packet) {
        if (origin != this.context.getParticipant().getControlAddress()) {
            this.context.getParticipant().updateRtcpAddress(origin);
            LOG.debug("Updated remote participant's RTCP address to {} in RtpSession with id {}.", origin, this.id);
        }

        for (RtpSessionControlListener listener : this.controlListeners) {
            listener.controlPacketReceived(this, packet);
        }
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipant getRemoteParticipant() {
        return this.context.getParticipant();
    }

    public boolean isIgnoreFromUnknownSsrc() {
        return ignoreFromUnknownSsrc;
    }

    public void setIgnoreFromUnknownSsrc(boolean ignoreFromUnknownSsrc) {
        this.ignoreFromUnknownSsrc = ignoreFromUnknownSsrc;
    }
}
