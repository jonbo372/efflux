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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class ControlPacketTest {

    @Test
    public void testDecodeCompoundPacket() throws Exception {
        // wireshark capture, 3 packets (SR, SDES, BYE), from X-lite
        byte[] firstPacketBytes = ByteUtils
                .convertHexStringToByteArray("80c80006e6aa996ed01f8460ea7ef9db001eb9b4000006e30004a084");
        byte[] secondPacketBytes = ByteUtils
                .convertHexStringToByteArray("81ca001ee6aa996e013d383232433634303536464438344539414231324438333442463" +
                                             "836303931354140756e697175652e7a333644423331373042303744344333302e6f7267" +
                                             "083110782d7274702d73657373696f6e2d6964363539413238344341443842344436313" +
                                             "83641324643304336383039363137300000");
        byte[] thirdPacketBytes = ByteUtils
                .convertHexStringToByteArray("81cb0001e6aa996e");

        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(firstPacketBytes, secondPacketBytes, thirdPacketBytes);

        List<ControlPacket> controlPackets = new ArrayList<ControlPacket>(3);
        while (buffer.readableBytes() > 0) {
            controlPackets.add(ControlPacket.decode(buffer));
        }

        assertEquals(0, buffer.readableBytes());
        assertEquals(3, controlPackets.size());

        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPackets.get(0).getType());
        assertEquals(ControlPacket.Type.SOURCE_DESCRIPTION, controlPackets.get(1).getType());
        assertEquals(ControlPacket.Type.BYE, controlPackets.get(2).getType());

        // No more tests needed as there is plenty of unit testing for each of those packets individually.
    }
}
