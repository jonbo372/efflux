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
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class ByePacketTest {

    @Test
    public void testDecode() throws Exception {
        // wireshark capture, X-lite
        byte [] packetBytes = ByteUtils.convertHexStringToByteArray("81cb0001e6aa996e");

        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.BYE, controlPacket.getType());

        ByePacket byePacket = (ByePacket) controlPacket;
        assertNotNull(byePacket.getSsrcList());
        assertEquals(1, byePacket.getSsrcList().size());
        assertEquals(new Long(0xe6aa996eL), byePacket.getSsrcList().get(0));
        assertEquals(null, byePacket.getReasonForLeaving());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testDecode2() throws Exception {
        // wireshark capture, jlibrtp
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("81cb000a4f52eb38156a6c69627274702073617973206279" +
                                                                   "6520627965210000000000000000000000000000");

        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.BYE, controlPacket.getType());

        ByePacket byePacket = (ByePacket) controlPacket;
        assertNotNull(byePacket.getSsrcList());
        assertEquals(1, byePacket.getSsrcList().size());
        assertEquals(new Long(0x4f52eb38L), byePacket.getSsrcList().get(0));
        assertEquals("jlibrtp says bye bye!", byePacket.getReasonForLeaving());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeDecode() throws Exception {
        ByePacket packet = new ByePacket();
        packet.addSsrc(0x45);
        packet.addSsrc(0x46);
        packet.setReasonForLeaving("So long, cruel world.");

        ChannelBuffer buffer = packet.encode();
        assertEquals(36, buffer.readableBytes());
        System.out.println(ByteUtils.writeArrayAsHex(buffer.array(), true));
        assertEquals(0, buffer.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(buffer);
        assertEquals(ControlPacket.Type.BYE, controlPacket.getType());

        ByePacket byePacket = (ByePacket) controlPacket;
        assertNotNull(byePacket.getSsrcList());
        assertEquals(2, byePacket.getSsrcList().size());
        assertEquals(new Long(0x45), byePacket.getSsrcList().get(0));
        assertEquals(new Long(0x46), byePacket.getSsrcList().get(1));
        assertEquals("So long, cruel world.", byePacket.getReasonForLeaving());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeDecodeWithFixedBlockSize64() throws Exception {
        ByePacket packet = new ByePacket();
        packet.addSsrc(0x45);
        packet.addSsrc(0x46);
        packet.setReasonForLeaving("So long, cruel world.");

        ChannelBuffer buffer = packet.encode(0, 64);
        assertEquals(64, buffer.readableBytes());
        byte[] bufferArray = buffer.array();
        System.out.println(ByteUtils.writeArrayAsHex(bufferArray, true));
        assertEquals(0, buffer.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(buffer);
        assertEquals(ControlPacket.Type.BYE, controlPacket.getType());

        ByePacket byePacket = (ByePacket) controlPacket;
        assertNotNull(byePacket.getSsrcList());
        assertEquals(2, byePacket.getSsrcList().size());
        assertEquals(new Long(0x45), byePacket.getSsrcList().get(0));
        assertEquals(new Long(0x46), byePacket.getSsrcList().get(1));
        assertEquals("So long, cruel world.", byePacket.getReasonForLeaving());

        // Size without fixed block size would be 36 so padding is 64 - 36
        assertEquals(64 - 36, bufferArray[bufferArray.length - 1]);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeDecodeWithFixedBlockSize64AndCompound() throws Exception {
        ByePacket packet = new ByePacket();
        packet.addSsrc(0x45);
        packet.addSsrc(0x46);
        packet.setReasonForLeaving("So long, cruel world.");

        ChannelBuffer buffer = packet.encode(60, 64);
        // Alignment would be to 128 bytes *with* the other RTCP packets. So this packet is sized at 128 - 60 = 68
        assertEquals(68, buffer.readableBytes());
        byte[] bufferArray = buffer.array();
        System.out.println(ByteUtils.writeArrayAsHex(bufferArray, true));
        assertEquals(0, buffer.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(buffer);
        assertEquals(ControlPacket.Type.BYE, controlPacket.getType());

        ByePacket byePacket = (ByePacket) controlPacket;
        assertNotNull(byePacket.getSsrcList());
        assertEquals(2, byePacket.getSsrcList().size());
        assertEquals(new Long(0x45), byePacket.getSsrcList().get(0));
        assertEquals(new Long(0x46), byePacket.getSsrcList().get(1));
        assertEquals("So long, cruel world.", byePacket.getReasonForLeaving());

        // Size without fixed block size would be 36 so padding is 128 - (60 + 36) because current compound length is 60
        assertEquals(128 - (60 + 36), bufferArray[bufferArray.length - 1]);
        assertEquals(0, buffer.readableBytes());
    }
}
