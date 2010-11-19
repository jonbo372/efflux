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

package com.biasedbit.efflux.session;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class MultiParticipantSessionTest {

    private MultiParticipantSession session;

    @After
    public void tearDown() {
        if (this.session != null) {
            this.session.terminate();
        }
    }

    @Test
    public void testNewParticipantFromDataPacket() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        participant.getInfo().setSsrc(6969);
        this.session = new MultiParticipantSession("id", 8, participant);
        assertTrue(this.session.init());

        this.session.addEventListener(new RtpSessionEventListener() {
            @Override
            public void participantJoinedFromData(RtpSession session, RtpParticipant participant) {
                assertEquals(69, participant.getSsrc());
            }

            @Override
            public void participantJoinedFromControl(RtpSession session, RtpParticipant participant) {
            }

            @Override
            public void participantDataUpdated(RtpSession session, RtpParticipant participant) {
            }

            @Override
            public void participantLeft(RtpSession session, RtpParticipant participant) {
            }

            @Override
            public void participantDeleted(RtpSession session, RtpParticipant participant) {
            }

            @Override
            public void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc) {
            }

            @Override
            public void sessionTerminated(RtpSession session, Throwable cause) {
                System.err.println("Session terminated: " + cause.getMessage());
            }
        });

        DataPacket packet = new DataPacket();
        packet.setSequenceNumber(1);
        packet.setPayloadType(8);
        packet.setSsrc(69);
        SocketAddress address = new InetSocketAddress("localhost", 8000);
        this.session.dataPacketReceived(address, packet);
    }

    @Test
    public void testOutOfOrderDiscard() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        participant.getInfo().setSsrc(6969);
        this.session = new MultiParticipantSession("id", 8, participant);
        this.session.setDiscardOutOfOrder(true);
        assertTrue(this.session.init());

        final AtomicInteger counter = new AtomicInteger(0);

        this.session.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                counter.incrementAndGet();
            }
        });

        DataPacket packet = new DataPacket();
        packet.setSequenceNumber(10);
        packet.setPayloadType(8);
        packet.setSsrc(69);
        SocketAddress address = new InetSocketAddress("localhost", 8000);
        this.session.dataPacketReceived(address, packet);
        packet.setSequenceNumber(11);
        this.session.dataPacketReceived(address, packet);
        packet.setSequenceNumber(10);
        this.session.dataPacketReceived(address, packet);

        assertEquals(2, counter.get());
    }
}
