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

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class DefaultRtpParticipantContext implements RtpParticipantContext {

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipant participant;

    // internal vars --------------------------------------------------------------------------------------------------

    private long byeReceptionInstant;
    private int lastSequenceNumber;
    private boolean receivedSdes;
    private final AtomicLong sentByteCounter;
    private final AtomicLong sentPacketCounter;
    private final AtomicLong receivedByteCounter;
    private final AtomicLong receivedPacketCounter;

    // constructors ---------------------------------------------------------------------------------------------------

    public DefaultRtpParticipantContext(RtpParticipant participant) {
        this.participant = participant;

        this.lastSequenceNumber = -1;
        this.byeReceptionInstant = -1;

        this.sentByteCounter = new AtomicLong();
        this.sentPacketCounter = new AtomicLong();
        this.receivedByteCounter = new AtomicLong();
        this.receivedPacketCounter = new AtomicLong();
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void byeReceived() {
        this.byeReceptionInstant = System.currentTimeMillis();
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    @Override
    public RtpParticipant getParticipant() {
        return participant;
    }

    @Override
    public long getByeReceptionInstant() {
        return byeReceptionInstant;
    }

    @Override
    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setLastSequenceNumber(int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    @Override
    public boolean receivedBye() {
        return this.byeReceptionInstant > -1;
    }

    @Override
    public long getSentPackets() {
        return this.sentPacketCounter.get();
    }

    @Override
    public long getSentBytes() {
        return this.sentByteCounter.get();
    }

    @Override
    public long getReceivedPackets() {
        return this.receivedPacketCounter.get();
    }

    @Override
    public long getReceivedBytes() {
        return this.receivedByteCounter.get();
    }
    
    // public methods -------------------------------------------------------------------------------------------------

    public void resetSendStats() {
        this.sentByteCounter.set(0);
        this.sentPacketCounter.set(0);
    }

    public long incrementSentBytes(int delta) {
        if (delta < 0) {
            return this.sentByteCounter.get();
        }

        return this.sentByteCounter.addAndGet(delta);
    }

    public long incrementSentPackets() {
        return this.sentPacketCounter.incrementAndGet();
    }

    public void receivedSdes() {
        this.receivedSdes = true;
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public boolean hasReceivedSdes() {
        return receivedSdes;
    }
}
