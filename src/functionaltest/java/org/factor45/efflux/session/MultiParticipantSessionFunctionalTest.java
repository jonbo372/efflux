package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;
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
        RtpParticipant[] p = new RtpParticipant[N];
        final AtomicInteger[] counters = new AtomicInteger[N];
        final CountDownLatch latch = new CountDownLatch(N);

        for (byte i = 0; i < N; i++) {
            p[i] = new RtpParticipant("127.0.0.1", 10000 + (i * 2), 20001 + (i * 2), i);
            this.sessions[i] = new MultiParticipantSession("session" + i, 8, p[i]);
            assertTrue(this.sessions[i].init());
            final AtomicInteger counter = new AtomicInteger();
            counters[i] = counter;
            this.sessions[i].addDataListener(new RtpSessionDataListener() {

                @Override
                public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                    System.err.println(session.getId() + " received data from " + participant + ": " + packet);
                    if (counter.incrementAndGet() == (N - 1)) {
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

                assertTrue(this.sessions[i].addParticipant(p[j]));
            }
        }

        for (byte i = 0; i < N; i++) {
            this.sessions[i].sendData(new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef}, i);
        }

        latch.await(5000L, TimeUnit.MILLISECONDS);

        for (byte i = 0; i < N; i++) {
            assertEquals(N - 1, counters[i].get());
        }
    }
}
