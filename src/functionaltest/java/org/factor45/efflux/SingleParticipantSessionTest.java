package org.factor45.efflux;

import org.factor45.efflux.packet.RtpPacket;
import org.factor45.efflux.session.RtpParticipant;
import org.factor45.efflux.session.RtpSession;
import org.factor45.efflux.session.RtpSessionDataListener;
import org.factor45.efflux.session.SingleParticipantSession;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class SingleParticipantSessionTest {

    private SingleParticipantSession session1;
    private SingleParticipantSession session2;

    @After
    public void tearDown() {
        if (this.session1 != null) {
            this.session1.terminate();
        }

        if (this.session2 != null) {
            this.session2.terminate();
        }
    }

    @Test
    public void testSendAndReceive() {
        final CountDownLatch latch = new CountDownLatch(2);

        RtpParticipant local1 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        RtpParticipant remote1 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        this.session1 = new SingleParticipantSession("Session1", 8, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
                latch.countDown();
            }
        });

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        RtpParticipant remote2 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session2 = new SingleParticipantSession("Session2", 8, local2, remote2);
        assertTrue(this.session2.init());
        this.session2.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                System.err.println("Session 2 received packet: " + packet + "(session: " + session.getId() + ")");
                latch.countDown();
            }
        });

        RtpPacket packet = new RtpPacket();
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45});
        packet.setSequenceNumber(1);
        assertTrue(this.session1.sendDataPacket(packet));
        packet.setSequenceNumber(2);
        assertTrue(this.session2.sendDataPacket(packet));

        try {
            assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Test
    public void testSendAndReceiveUpdatingRemote() {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        String initialHost;

        RtpParticipant local1 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        RtpParticipant remote1 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        this.session1 = new SingleParticipantSession("Session1", 8, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
                latch2.countDown();
            }
        });

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        RtpParticipant remote2 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session2 = new SingleParticipantSession("Session2", 8, local2, remote2);
        assertTrue(this.session2.init());
        this.session2.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet) {
                System.err.println("Session 2 received packet: " + packet + "(session: " + session.getId() + ")");
                latch.countDown();
            }
        });

        assertEquals("/127.0.0.1:7000", this.session1.getRemoteParticipant().getDataAddress().toString());
        assertEquals("/127.0.0.1:9000", this.session2.getRemoteParticipant().getDataAddress().toString());
        initialHost = this.session2.getRemoteParticipant().getDataAddress().toString();

        RtpPacket packet = new RtpPacket();
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45});
        packet.setSequenceNumber(1);
        assertTrue(this.session1.sendDataPacket(packet));
        packet.setSequenceNumber(2);
        assertTrue(this.session2.sendDataPacket(packet));

        try {
            assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        assertEquals("/127.0.0.1:7000", this.session1.getRemoteParticipant().getDataAddress().toString());
        assertEquals("/127.0.0.1:6000", this.session2.getRemoteParticipant().getDataAddress().toString());

        System.err.println("Updated remote participant's address from " + initialHost + " to " +
                           this.session2.getRemoteParticipant().getDataAddress().toString());

        packet.setSequenceNumber(3);
        assertTrue(this.session2.sendDataPacket(packet));

        try {
            assertTrue(latch2.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

    }
}
