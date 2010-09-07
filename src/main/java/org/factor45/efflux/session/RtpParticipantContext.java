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

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class RtpParticipantContext {

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipant participant;

    // internal vars --------------------------------------------------------------------------------------------------

    private long byeReceptionInstant;
    private int lastSequenceNumber;

    // constructors ---------------------------------------------------------------------------------------------------

    public RtpParticipantContext(RtpParticipant participant) {
        this.participant = participant;

        this.lastSequenceNumber = -1;
        this.byeReceptionInstant = -1;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void byeReceived() {
        this.byeReceptionInstant = System.currentTimeMillis();
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipant getParticipant() {
        return participant;
    }

    public long getByeReceptionInstant() {
        return byeReceptionInstant;
    }

    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setLastSequenceNumber(int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }
}
