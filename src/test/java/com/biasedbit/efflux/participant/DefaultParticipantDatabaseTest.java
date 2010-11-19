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

package com.biasedbit.efflux.participant;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.packet.SdesChunk;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class DefaultParticipantDatabaseTest {

    private DefaultParticipantDatabase database;
    private TestListener listener;

    @Before
    public void setUp() throws Exception {
        this.listener = new TestListener();
        this.database = new DefaultParticipantDatabase("testDatabase", this.listener);
    }

    @Test
    public void testGetId() throws Exception {
        // Ow, ffs...
        assertEquals("testDatabase", this.database.getId());
    }

    @Test
    public void testAddReceiver() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        assertEquals(0, this.database.getReceiverCount());
        assertTrue(this.database.addReceiver(participant));
        assertEquals(1, this.database.getReceiverCount());
    }

    @Test
    public void testFailAddReceiver() throws Exception {
        SdesChunk chunk = new SdesChunk(0x45);
        SocketAddress address = new InetSocketAddress("localhost", 8000);
        // Creation of a non-receiver participant, and adding it as a receiver must fail.
        RtpParticipant participant = RtpParticipant.createFromSdesChunk(address, chunk);
        assertEquals(0, this.database.getReceiverCount());
        assertFalse(this.database.addReceiver(participant));
        assertEquals(0, this.database.getReceiverCount());
    }

    @Test
    public void testDoWithReceivers() throws Exception {
        this.testAddReceiver();

        final AtomicBoolean doSomething = new AtomicBoolean();
        this.database.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                doSomething.set(true);
            }
        });

        assertTrue(doSomething.get());
    }

    @Test
    public void testRemoveReceiver() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        assertEquals(0, this.database.getReceiverCount());
        assertTrue(this.database.addReceiver(participant));
        assertEquals(1, this.database.getReceiverCount());
        assertTrue(this.database.removeReceiver(participant));
        assertEquals(0, this.database.getReceiverCount());
    }

    @Test
    public void testRemoveReceiver2() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        assertEquals(0, this.database.getReceiverCount());
        assertTrue(this.database.addReceiver(participant));
        assertEquals(1, this.database.getReceiverCount());

        // Not added yet, so won't be removed.
        participant = RtpParticipant.createReceiver("localhost", 8002, 8003);
        assertFalse(this.database.removeReceiver(participant));
        assertEquals(1, this.database.getReceiverCount());
    }

    @Test
    public void testGetOrCreateParticipantFromDataPacket() throws Exception {
        DataPacket packet = new DataPacket();
        packet.setSsrc(0x45);
        SocketAddress address = new InetSocketAddress("localhost", 8000);

        assertEquals(0, this.database.getReceiverCount());
        assertEquals(0, this.database.getParticipantCount());
        RtpParticipant participant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
        assertNotNull(participant);
        assertEquals(1, this.database.getParticipantCount());
        assertEquals(0, this.database.getReceiverCount());
        assertEquals(0x45, participant.getSsrc());
        assertNotNull(participant.getLastDataOrigin());
        assertEquals(address, participant.getLastDataOrigin());
        assertNull(participant.getLastControlOrigin());
        assertEquals(0, this.listener.getSdesCreations());
        assertEquals(1, this.listener.getDataPacketCreations());
        assertEquals(0, this.listener.getDeletions());
    }

    @Test
    public void testGetOrCreateParticipantFromSdesChunk() throws Exception {
        SdesChunk chunk = new SdesChunk(0x45);
        SocketAddress address = new InetSocketAddress("localhost", 8000);

        assertEquals(0, this.database.getReceiverCount());
        assertEquals(0, this.database.getParticipantCount());
        RtpParticipant participant = this.database.getOrCreateParticipantFromSdesChunk(address, chunk);
        assertNotNull(participant);
        assertEquals(1, this.database.getParticipantCount());
        assertEquals(0, this.database.getReceiverCount());
        assertEquals(0x45, participant.getSsrc());
        assertNull(participant.getLastDataOrigin());
        assertNotNull(participant.getLastControlOrigin());
        assertEquals(address, participant.getLastControlOrigin());
        assertEquals(1, this.listener.getSdesCreations());
        assertEquals(0, this.listener.getDataPacketCreations());
        assertEquals(0, this.listener.getDeletions());
    }

    @Test
    public void testAssociationOfParticipantViaDataAddress() throws Exception {
        RtpParticipant receiver = RtpParticipant.createReceiver("localhost", 8000, 8001);
        assertEquals(0, this.database.getReceiverCount());
        assertTrue(this.database.addReceiver(receiver));
        assertEquals(1, this.database.getReceiverCount());

        DataPacket packet = new DataPacket();
        packet.setSsrc(0x45);
        SocketAddress address = new InetSocketAddress("localhost", 8000);

        assertEquals(0, this.database.getParticipantCount());
        RtpParticipant participant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
        assertNotNull(participant);
        assertEquals(1, this.database.getParticipantCount());
        assertEquals(1, this.database.getReceiverCount());
        assertEquals(0x45, participant.getSsrc());
        assertNotNull(participant.getLastDataOrigin());
        // Here is where the association is tested.
        assertEquals(receiver, participant);
        assertEquals(0, this.listener.getSdesCreations());
        assertEquals(0, this.listener.getDataPacketCreations());
        assertEquals(0, this.listener.getDeletions());
    }

    @Test
    public void testNonAssociationOfParticipantViaDataAddress() throws Exception {
        RtpParticipant receiver = RtpParticipant.createReceiver("localhost", 8000, 8001);
        assertEquals(0, this.database.getReceiverCount());
        assertTrue(this.database.addReceiver(receiver));
        assertEquals(1, this.database.getReceiverCount());

        DataPacket packet = new DataPacket();
        packet.setSsrc(0x45);
        SocketAddress address = new InetSocketAddress("localhost", 9000);

        assertEquals(0, this.database.getParticipantCount());
        RtpParticipant participant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
        assertNotNull(participant);
        assertEquals(1, this.database.getParticipantCount());
        assertEquals(1, this.database.getReceiverCount());
        assertEquals(0x45, participant.getSsrc());
        assertNotNull(participant.getLastDataOrigin());
        // Here is where the association is tested.
        assertNotSame(receiver, participant);
        assertEquals(0, this.listener.getSdesCreations());
        assertEquals(1, this.listener.getDataPacketCreations());
        assertEquals(0, this.listener.getDeletions());
    }

    @Test
    public void testCleanup() throws Exception {
    }

    // private classes ------------------------------------------------------------------------------------------------

    private static class TestListener implements ParticipantEventListener {

        // internal vars ----------------------------------------------------------------------------------------------

        private final AtomicInteger sdesCreations = new AtomicInteger();
        private final AtomicInteger dataPacketCreations = new AtomicInteger();
        private final AtomicInteger deletions = new AtomicInteger();

        // ParticipantEventListener -----------------------------------------------------------------------------------

        @Override
        public void participantCreatedFromSdesChunk(RtpParticipant participant) {
            this.sdesCreations.incrementAndGet();
        }

        @Override
        public void participantCreatedFromDataPacket(RtpParticipant participant) {
            this.dataPacketCreations.incrementAndGet();
        }

        @Override
        public void participantDeleted(RtpParticipant participant) {
            this.deletions.incrementAndGet();
        }

        // public methods ---------------------------------------------------------------------------------------------

        public int getSdesCreations() {
            return this.sdesCreations.get();
        }

        public int getDataPacketCreations() {
            return this.dataPacketCreations.get();
        }

        public int getDeletions() {
            return this.deletions.get();
        }
    }
}
