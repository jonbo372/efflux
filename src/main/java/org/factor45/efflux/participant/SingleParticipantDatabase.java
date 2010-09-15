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

package org.factor45.efflux.participant;

import org.factor45.efflux.packet.DataPacket;
import org.factor45.efflux.packet.SdesChunk;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class SingleParticipantDatabase implements ParticipantDatabase {

    // configuration --------------------------------------------------------------------------------------------------

    private String id;
    private RtpParticipant participant;

    // constructors ---------------------------------------------------------------------------------------------------

    public SingleParticipantDatabase(String id, RtpParticipant participant) {
        this.id = id;
    }

    // ParticipantDatabase --------------------------------------------------------------------------------------------

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Collection<RtpParticipant> getReceivers() {
        return Arrays.asList(this.participant);
    }

    @Override
    public Map<Long, RtpParticipant> getMembers() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doWithReceivers(ParticipantOperation operation) {
        try {
            operation.doWithParticipant(this.participant);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void doWithParticipants(ParticipantOperation operation) {
        try {
            operation.doWithParticipant(this.participant);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public boolean addReceiver(RtpParticipant remoteParticipant) {
        return remoteParticipant == this.participant;
    }

    @Override
    public boolean removeReceiver(RtpParticipant remoteParticipant) {
        return false;
    }

    @Override
    public RtpParticipant getParticipant(long ssrc) {
        if (ssrc == this.participant.getSsrc()) {
            return this.participant;
        }

        return null;
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromDataPacket(SocketAddress origin, DataPacket packet) {
        if (packet.getSsrc() == this.participant.getSsrc()) {
            return this.participant;
        }

        return null;
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromSdesChunk(SocketAddress origin, SdesChunk chunk) {
        if (chunk.getSsrc() == this.participant.getSsrc()) {
            return this.participant;
        }

        return null;
    }

    @Override
    public int getReceiverCount() {
        return 1;
    }

    @Override
    public int getParticipantCount() {
        return 1;
    }

    @Override
    public void cleanup() {
        // Nothing to do here.
    }
}
