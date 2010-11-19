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

package com.biasedbit.efflux.packet;

import com.biasedbit.efflux.util.ByteUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class SenderReportPacketTest {

    @Test
    public void testDecode() throws Exception {
        // wireshark capture, from X-lite
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("80c800064f52eb38d01f84417f3b6459a91e7bd9000000020" +
                                                                   "0000002");


        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;

        assertEquals(0x4f52eb38L, srPacket.getSenderSsrc());
        assertEquals(2837347289L, srPacket.getRtpTimestamp());
        assertEquals(2, srPacket.getSenderPacketCount());
        assertEquals(2, srPacket.getSenderOctetCount());
        assertEquals(0, srPacket.getReceptionReportCount());
        assertNull(srPacket.getReceptionReports());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testDecode2() throws Exception {
        // wireshark capture, from jlibrtp
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("80c80006e6aa996ed01f84481be76c8b001bb2b40000020b0" +
                                                                   "0015f64");

        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;

        assertEquals(0xe6aa996eL, srPacket.getSenderSsrc());
        assertEquals(1815220L, srPacket.getRtpTimestamp());
        assertEquals(523, srPacket.getSenderPacketCount());
        assertEquals(89956, srPacket.getSenderOctetCount());
        assertEquals(0, srPacket.getReceptionReportCount());
        assertNull(srPacket.getReceptionReports());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeDecode() throws Exception {
        SenderReportPacket packet = new SenderReportPacket();
        packet.setSenderSsrc(0x45);
        packet.setNtpTimestamp(0x45);
        packet.setRtpTimestamp(0x45);
        packet.setSenderOctetCount(20);
        packet.setSenderPacketCount(2);
        ReceptionReport block = new ReceptionReport();
        block.setSsrc(10);
        block.setCumulativeNumberOfPacketsLost(11);
        block.setFractionLost((short) 12);
        block.setDelaySinceLastSenderReport(13);
        block.setInterArrivalJitter(14);
        block.setExtendedHighestSequenceNumberReceived(15);
        packet.addReceptionReportBlock(block);
        block = new ReceptionReport();
        block.setSsrc(20);
        block.setCumulativeNumberOfPacketsLost(21);
        block.setFractionLost((short) 22);
        block.setDelaySinceLastSenderReport(23);
        block.setInterArrivalJitter(24);
        block.setExtendedHighestSequenceNumberReceived(25);
        packet.addReceptionReportBlock(block);

        ChannelBuffer encoded = packet.encode();
        assertEquals(0, encoded.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(encoded);
        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;

        assertEquals(0x45, srPacket.getNtpTimestamp());
        assertEquals(0x45, srPacket.getRtpTimestamp());
        assertEquals(20, srPacket.getSenderOctetCount());
        assertEquals(2, srPacket.getSenderPacketCount());
        assertNotNull(srPacket.getReceptionReports());
        assertEquals(2, srPacket.getReceptionReportCount());
        assertEquals(2, srPacket.getReceptionReports().size());
        assertEquals(10, srPacket.getReceptionReports().get(0).getSsrc());
        assertEquals(11, srPacket.getReceptionReports().get(0).getCumulativeNumberOfPacketsLost());
        assertEquals(12, srPacket.getReceptionReports().get(0).getFractionLost());
        assertEquals(13, srPacket.getReceptionReports().get(0).getDelaySinceLastSenderReport());
        assertEquals(14, srPacket.getReceptionReports().get(0).getInterArrivalJitter());
        assertEquals(15, srPacket.getReceptionReports().get(0).getExtendedHighestSequenceNumberReceived());
        assertEquals(20, srPacket.getReceptionReports().get(1).getSsrc());
        assertEquals(21, srPacket.getReceptionReports().get(1).getCumulativeNumberOfPacketsLost());
        assertEquals(22, srPacket.getReceptionReports().get(1).getFractionLost());
        assertEquals(23, srPacket.getReceptionReports().get(1).getDelaySinceLastSenderReport());
        assertEquals(24, srPacket.getReceptionReports().get(1).getInterArrivalJitter());
        assertEquals(25, srPacket.getReceptionReports().get(1).getExtendedHighestSequenceNumberReceived());

        assertEquals(0, encoded.readableBytes());
    }
}
