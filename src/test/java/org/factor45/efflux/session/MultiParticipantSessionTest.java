package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;
import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
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
        RtpParticipant participant = new RtpParticipant("localhost", 8000, 8001, 6969);
        this.session = new MultiParticipantSession("id", 8, participant);
        assertTrue(this.session.init());

        final CountDownLatch latch = new CountDownLatch(1);

        this.session.addEventListener(new RtpSessionEventListener() {
            @Override
            public void participantJoinedFromData(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                assertEquals(69, participant.getSynchronisationSourceId());
            }

            @Override
            public void participantJoinedFromControl(RtpSession session, RtpParticipant participant, RtpPacket packet) {
            }

            @Override
            public void participantLeft(RtpSession session, RtpParticipant participant) {
            }
        });

        RtpPacket packet = new RtpPacket();
        packet.setSequenceNumber(1);
        packet.setPayloadType(8);
        packet.setSynchronisationSourceId(69);
        SocketAddress address = new InetSocketAddress("localhost", 8000);
        this.session.dataPacketReceived(address, packet);
    }

    @Test
    public void testOutOfOrderDiscard() throws Exception {
        RtpParticipant participant = new RtpParticipant("localhost", 8000, 8001, 6969);
        this.session = new MultiParticipantSession("id", 8, participant);
        assertTrue(this.session.init());

        final AtomicInteger counter = new AtomicInteger(0);

        this.session.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                counter.incrementAndGet();
            }
        });

        RtpPacket packet = new RtpPacket();
        packet.setSequenceNumber(10);
        packet.setPayloadType(8);
        packet.setSynchronisationSourceId(69);
        SocketAddress address = new InetSocketAddress("localhost", 8000);
        this.session.dataPacketReceived(address, packet);
        packet.setSequenceNumber(11);
        this.session.dataPacketReceived(address, packet);
        packet.setSequenceNumber(10);
        this.session.dataPacketReceived(address, packet);

        assertEquals(2, counter.get());
    }
}
