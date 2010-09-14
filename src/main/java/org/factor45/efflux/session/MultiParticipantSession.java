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

import java.util.Collections;
import java.util.Map;

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

    // constructors ---------------------------------------------------------------------------------------------------

    public MultiParticipantSession(String id, int payloadType, RtpParticipant localParticipant) {
        super(id, payloadType, localParticipant);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipantContext getParticipantContext(long ssrc) {
        return this.participantTable.get(ssrc);
    }

    public Map<Long, RtpParticipantContext> getParticipantTable() {
        return Collections.<Long, RtpParticipantContext>unmodifiableMap(this.participantTable);
    }
}
