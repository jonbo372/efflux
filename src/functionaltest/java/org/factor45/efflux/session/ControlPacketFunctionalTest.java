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

import org.factor45.efflux.packet.AppDataPacket;
import org.factor45.efflux.packet.ByePacket;
import org.factor45.efflux.packet.CompoundControlPacket;
import org.factor45.efflux.packet.DataPacket;
import org.factor45.efflux.packet.SdesChunk;
import org.factor45.efflux.packet.SdesChunkItems;
import org.factor45.efflux.packet.SourceDescriptionPacket;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class ControlPacketFunctionalTest {

    private AbstractRtpSession session1;
    private AbstractRtpSession session2;

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
        this.session1.setAutomatedRtcpHandling(false);
        assertTrue(this.session1.init());
        this.session1.addControlListener(new RtpSessionControlListener() {
            @Override
            public void controlPacketReceived(RtpSession session, CompoundControlPacket packet) {
                System.err.println("Session 1 received rtcp packet:\n" + packet);
                latch.countDown();
            }

            @Override
            public void appDataReceived(RtpSession session, AppDataPacket appDataPacket) {
                fail("Unexpected APP_DATA packet received");
            }
        });

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        RtpParticipant remote2 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session2 = new SingleParticipantSession("Session2", 8, local2, remote2);
        this.session2.setAutomatedRtcpHandling(false);
        assertTrue(this.session2.init());

        SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        SdesChunk chunk = new SdesChunk();
        chunk.setSsrc(2);
        chunk.addItem(SdesChunkItems.createCnameItem("session2@127.0.0.1:7000"));
        chunk.addItem(SdesChunkItems.createNameItem("session2"));
        sdesPacket.addItem(chunk);

        assertTrue(this.session2.sendControlPacket(sdesPacket));

        ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(2);
        byePacket.setReasonForLeaving("weeeeeeell it's about time to be hittin' the old dusty trail.");
        CompoundControlPacket compoundPacket = new CompoundControlPacket(sdesPacket, byePacket);

        assertTrue(this.session2.sendControlPacket(compoundPacket));

        try {
            assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Test
    public void testSendAndNotReceiveForAutomatedRtcpSession() {
        final CountDownLatch latch = new CountDownLatch(1);

        RtpParticipant local1 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session1 = new MultiParticipantSession("Session1", 8, local1);
        this.session1.setAutomatedRtcpHandling(true);
        assertTrue(this.session1.init());
        this.session1.addControlListener(new RtpSessionControlListener() {
            @Override
            public void controlPacketReceived(RtpSession session, CompoundControlPacket packet) {
                fail("Shouldn't have received this packet...");
            }

            @Override
            public void appDataReceived(RtpSession session, AppDataPacket appDataPacket) {
                fail("Unexpected APP_DATA packet received");
            }
        });

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        RtpParticipant remote2 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session2 = new SingleParticipantSession("Session2", 8, local2, remote2);
        this.session2.setAutomatedRtcpHandling(false);
        assertTrue(this.session2.init());

        SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        SdesChunk chunk = new SdesChunk();
        chunk.setSsrc(2);
        chunk.addItem(SdesChunkItems.createCnameItem("session2@127.0.0.1:7000"));
        chunk.addItem(SdesChunkItems.createNameItem("session2"));
        sdesPacket.addItem(chunk);

        assertTrue(this.session2.sendControlPacket(sdesPacket));

        ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(2);
        byePacket.setReasonForLeaving("weeeeeeell it's about time to be hittin' the old dusty trail.");
        CompoundControlPacket compoundPacket = new CompoundControlPacket(sdesPacket, byePacket);

        assertTrue(this.session2.sendControlPacket(compoundPacket));

        try {
            assertFalse(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Test
    public void testSendSdesAndByePackets() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        RtpParticipant local1 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session1 = new MultiParticipantSession("Session1", 8, local1);
        this.session1.addEventListener(new RtpSessionEventListener() {
            @Override
            public void participantJoinedFromData(RtpSession session, RtpParticipant participant, DataPacket packet) {
                fail("Unexpected event triggered.");
            }

            @Override
            public void participantJoinedFromControl(RtpSession session, RtpParticipant participant, SdesChunk chunk) {
                System.err.println("Participant joined from SDES chunk: " + chunk);
                latch.countDown();
            }

            @Override
            public void participantDataUpdated(RtpSession session, RtpParticipant participant) {
                fail("Unexpected event triggered.");
            }

            @Override
            public void participantLeft(RtpSession session, RtpParticipant participant) {
                System.err.println("Participant left: " + participant);
                latch.countDown();
            }

            @Override
            public void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc) {
                fail("Unexpected event triggered.");
            }

            @Override
            public void sessionTerminated(RtpSession session, Throwable cause) {
                System.err.println("Session terminated: " + cause.getMessage());
            }
        });
        assertTrue(this.session1.init());

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        this.session2 = new MultiParticipantSession("Session2", 8, local2);
        this.session2.addParticipant(local1);
        assertTrue(this.session2.init());

        this.session2.terminate();

        try {
            assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Test
    public void testUpdateSdes() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        RtpParticipant local1 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session1 = new MultiParticipantSession("Session1", 8, local1);
        this.session1.addEventListener(new RtpSessionEventListener() {
            @Override
            public void participantJoinedFromData(RtpSession session, RtpParticipant participant, DataPacket packet) {
                System.err.println("Participant joined from DataPacket: " + packet);
                latch.countDown();
            }

            @Override
            public void participantJoinedFromControl(RtpSession session, RtpParticipant participant, SdesChunk chunk) {
                fail("Unexpected packet received");
            }

            @Override
            public void participantDataUpdated(RtpSession session, RtpParticipant participant) {
                System.err.println("Participant information updated: " + participant);
                latch.countDown();
            }

            @Override
            public void participantLeft(RtpSession session, RtpParticipant participant) {
                System.err.println("Participant left: " + participant);
            }

            @Override
            public void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc) {
            }

            @Override
            public void sessionTerminated(RtpSession session, Throwable cause) {
                System.err.println("Session terminated: " + cause.getMessage());
            }
        });
        assertTrue(this.session1.init());

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        this.session2 = new MultiParticipantSession("Session2", 8, local2);
        this.session2.addParticipant(local1);
        this.session2.setAutomatedRtcpHandling(false);
        assertTrue(this.session2.init());

        assertTrue(this.session2.sendData(new byte[]{0x45, 0x45, 0x45}, 0, false));
        SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        SdesChunk chunk = new SdesChunk();
        chunk.setSsrc(2);
        chunk.addItem(SdesChunkItems.createCnameItem("session2@127.0.0.1:7000"));
        chunk.addItem(SdesChunkItems.createNameItem("session2"));
        sdesPacket.addItem(chunk);

        assertTrue(this.session2.sendControlPacket(sdesPacket));

        this.session2.terminate();

        try {
            assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        RtpParticipantContext context = this.session1.getRemoteParticipant(2);
        assertNotNull(context);
        RtpParticipant participant = context.getParticipant();
        assertEquals(2, participant.getSsrc());
        assertEquals("session2@127.0.0.1:7000", participant.getCname());
        assertEquals("session2", participant.getName());
    }
}
