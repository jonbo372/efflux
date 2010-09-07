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

import org.factor45.efflux.packet.ByePacket;
import org.factor45.efflux.packet.CompoundControlPacket;
import org.factor45.efflux.packet.SdesChunk;
import org.factor45.efflux.packet.SdesChunkItems;
import org.factor45.efflux.packet.SourceDescriptionPacket;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertTrue(this.session1.init());
        this.session1.addControlListener(new RtpSessionControlListener() {
            @Override
            public void controlPacketReceived(RtpSession session, CompoundControlPacket packet) {
                System.err.println("Session 1 received rtcp packet:\n" + packet);
                latch.countDown();
            }
        });

        RtpParticipant local2 = new RtpParticipant("127.0.0.1", 7000, 7001, 2);
        RtpParticipant remote2 = new RtpParticipant("127.0.0.1", 6000, 6001, 1);
        this.session2 = new SingleParticipantSession("Session2", 8, local2, remote2);
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
}
