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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class MultiParticipantSessionFunctionalTest {

    private static final byte N = 5;

    private MultiParticipantSession[] sessions;

    @After
    public void tearDown() {
        if (this.sessions != null) {
            for (MultiParticipantSession session : this.sessions) {
                if (session != null) {
                    session.terminate();
                }
            }
        }
    }

    @Test
    public void testDeliveryToAllParticipants() throws Exception {
        this.sessions = new MultiParticipantSession[N];
        final AtomicInteger[] counters = new AtomicInteger[N];
        final CountDownLatch latch = new CountDownLatch(N);

        for (byte i = 0; i < N; i++) {
            RtpParticipant participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(i), "127.0.0.1", 10000 + (i * 2), 20001 + (i * 2));
            this.sessions[i] = new MultiParticipantSession("session" + i, 8, participant);
            assertTrue(this.sessions[i].init());
            final AtomicInteger counter = new AtomicInteger();
            counters[i] = counter;
            this.sessions[i].addDataListener(new RtpSessionDataListener() {

                @Override
                public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                    System.err.println(session.getId() + " received data from " + participant + ": " + packet);
                    if (counter.incrementAndGet() == ((N - 1) * 2)) {
                        // Release the latch once all N-1 messages (because it wont receive the message it sends) are
                        // received.
                        latch.countDown();
                    }
                }
            });
        }

        // All sessions set up, now add all participants to the other sessions
        for (byte i = 0; i < N; i++) {
            for (byte j = 0; j < N; j++) {
                if (j == i) {
                    continue;
                }

                // You can NEVER add the same participant to two distinct sessions otherwise you'll ruin it (as both
                // will be messing in the same participant).
                RtpParticipant participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(j), "127.0.0.1", 10000 + (j * 2), 20001 + (j * 2));
                System.err.println("Adding " + participant + " to session " + this.sessions[i].getId());
                assertTrue(this.sessions[i].addReceiver(participant));
            }
        }

        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        for (byte i = 0; i < N; i++) {
            assertTrue(this.sessions[i].sendData(deadbeef, 0x45, false));
            assertTrue(this.sessions[i].sendData(deadbeef, 0x45, false));
        }

        latch.await(5000L, TimeUnit.MILLISECONDS);

        for (byte i = 0; i < N; i++) {
            assertEquals(((N - 1) * 2), counters[i].get());
        }
    }
}
