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
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
public class DataPacketTest {

    public static final byte[] ALAW_RTP_PACKET_SAMPLE =
            {(byte) 0x80, (byte) 0x88, 0x19, 0x73, 0x00, 0x01, (byte) 0x95, 0x14, 0x1f, (byte) 0xcc, 0x77,
                       (byte) 0x9a, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5};

    public static final String H263_PACKET = "80a2123800130ecf4fbc4ca10040000000009842d8ceb128ab51a2ec38ada901890329" +
                                             "d48c18877be2d9729b7312ef309d6ee3167a030e06c0d5f421d03b8b60b619cfde18fc" +
                                             "857c7ef3e413b4338181f0a245ea51c3dc4f4500776ca94702f693dff03b756a123da2" +
                                             "597402ee6e7b497550afdf5ab9fffde463fc85d75ae0c4c3c5ea54a9f8f1462d2743d3" +
                                             "a6e0c0792bc2ef45e8c17810deefef6ba33ffc7bd4c20e58a76a32049120bfd9b2eb45" +
                                             "a2a662fffffe38e1ea5e5d1a10868d87a5f4ebcf7e5f00fff1a9267d868d12095f1e97" +
                                             "4a9462e4ac4b57f987d2c180111fd83e2f1ef54282db454e5f540e101477be77ac0301" +
                                             "df80c0fc81287df80218c1055c5624ff1aaa018b03f6969c797be40adfcf429781f9ed" +
                                             "1f0f810fe0607be548f4337a352824806021dbf0607e44955ac2a2fb3c2235e698d4b4" +
                                             "ebe3dfdf3e0860c078d06103d0892aa15c8b152ee9e0430603c64d06043818af82e067" +
                                             "bff86e7818110520c27c0920c4fdabab2f6ac5a90f4684204054245d03e3ffda0a72fb" +
                                             "6206b8c19831e55e811560e6febe82c800009c46569616cf2b9e847ec2d1e8b1255e8f" +
                                             "d5a5c40715a86be13ffd0fffc4a5182db2f8675aaffcf44a5b45582adf0503a8506003" +
                                             "fb3e9a78b548b19e2c18014df8303f20c4024072ff58e970670305db1ef17f8f72bef8" +
                                             "611425aa1fb70b2c16bfc13a5903e08610954053349f4db73d890ae0f07deffe42ff0f" +
                                             "628aafdb54ea7e9866f1b7154f9356e3a9975505d7d4456d3e538b225abaad50f35a6a" +
                                             "20162741e4054d4050f4bb8c8fb8941ce027098d30f7732cbefa855fee4aa350ef866c" +
                                             "65fe9f6a1e70ad24bb80679632054fb55352be73dec7e5f14fdf1607e12cb09707fdbc" +
                                             "48a7d01ccfec112c49fc9951e093e9f4540ec4bb3b0e3d3e06187c6028637d4dc0ec40" +
                                             "8f8985f2a431b60c0fcd0538fd1843480e3025f80651fd052d57ac7c49970dc0cfda22" +
                                             "625ae7d21efe3d8d987b977a62407c257ffdfabc467dda172f751bde1f800000a04616" +
                                             "54c101eae3df062d8de7e1df3af8925f15090ad5ccdcc9d9bb474d3666aabdf020d902" +
                                             "17d7fe48047418c027d5ccbfca17427ffb657ca8a1547e3067520c079aaf17092afdeb" +
                                             "6c1d369fc1962b7ff45df0502e238180110865c1084aa3d83b56aefdb54d350475713b" +
                                             "065a558301e63e1e6c653fb989f540395fd714a5fc084aa0700e681431f7e3cf87560c" +
                                             "079aa4c0c4fe0f81cdfda01f820eb40c5fd8f81cdfdcb0cf5df087df8f38d52f0607e7" +
                                             "e5e3d83f932cbe060be957312066be5d9ff4ffbb2c3fc2d6103c0c105891e537c0c102" +
                                             "97031604d69b7c54d9043979a7aa4d30a5a5ef5a03c3f82317d1ab49789610c4ac523f" +
                                             "6da99e4661b8692e89ef56b73dbfcf45ff72217c20410823ed0396fe46ffe9cea7d744" +
                                             "cf500f12e7c4b6fe23e718c73aab040f08d999df95950c5b309e4f70d3ea1597c5728e" +
                                             "fea93a8d17e9d00c5568307efa0c402839dfb4e25aa9ea3a3992ae61a7757e2685f53f" +
                                             "6d3560c00745580c17d37a0c202018b80a77e9972a042fef87e894582db500f0522307" +
                                             "300b43fdf696de04157e031508b2aa0301dc5eb3b474205e80";

    @Test
    public void testDecode() {
        DataPacket packet = DataPacket.decode(ALAW_RTP_PACKET_SAMPLE);
        assertEquals(RtpVersion.V2, packet.getVersion());
        assertFalse(packet.hasExtension());
        assertEquals(0, packet.getContributingSourcesCount());
        assertTrue(packet.hasMarker());
        assertEquals(8, packet.getPayloadType());
        assertEquals(6515, packet.getSequenceNumber());
        assertEquals(103700, packet.getTimestamp());
        assertEquals(0x1fcc779a, packet.getSsrc());
        assertEquals(6, packet.getDataSize());
    }

    @Test
    public void testEncode() {
        DataPacket packet = new DataPacket();
        packet.setVersion(RtpVersion.V2);
        packet.setMarker(true);
        packet.setPayloadType(8);
        packet.setSequenceNumber(6515);
        packet.setTimestamp(103700);
        packet.setSsrc(0x1fcc779a);
        packet.setData(new byte[]{(byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5});
        ChannelBuffer buffer = packet.encode();
        assertTrue(Arrays.equals(ALAW_RTP_PACKET_SAMPLE, buffer.array()));
    }

    @Test
    public void testEncodeDecode() {
        DataPacket packet = new DataPacket();
        packet.setVersion(RtpVersion.V2);
        packet.setMarker(true);
        packet.setPayloadType(98);
        packet.setSequenceNumber(69);
        packet.setTimestamp(696969);
        packet.setSsrc(96);
        packet.setExtensionHeader((short) 0x8080, new byte[]{0x70, 0x70, 0x70, 0x70});
        packet.addContributingSourceId(69);
        packet.addContributingSourceId(70);
        packet.addContributingSourceId(71);
        packet.setData(new byte[]{0x69, 0x69, 0x69, 0x69});

        ChannelBuffer buffer = packet.encode();

        DataPacket decoded = DataPacket.decode(buffer);
        assertEquals(packet.getVersion(), decoded.getVersion());
        assertEquals(packet.hasMarker(), decoded.hasMarker());
        assertEquals(packet.getPayloadType(), decoded.getPayloadType());
        assertEquals(packet.getSequenceNumber(), decoded.getSequenceNumber());
        assertEquals(packet.getTimestamp(), decoded.getTimestamp());
        assertEquals(packet.getSsrc(), decoded.getSsrc());
        assertEquals(packet.getExtensionDataSize(), decoded.getExtensionDataSize());
        assertEquals(packet.getExtensionHeaderData(), decoded.getExtensionHeaderData());
        assertTrue(Arrays.equals(packet.getExtensionData(), packet.getExtensionData()));
        assertEquals(packet.getContributingSourcesCount(), decoded.getContributingSourcesCount());
        assertEquals(packet.getContributingSourceIds().get(0), decoded.getContributingSourceIds().get(0));
        assertEquals(packet.getContributingSourceIds().get(1), decoded.getContributingSourceIds().get(1));
        assertEquals(packet.getContributingSourceIds().get(2), decoded.getContributingSourceIds().get(2));
        assertEquals(packet.getDataSize(), decoded.getDataSize());
        assertTrue(Arrays.equals(packet.getDataAsArray(), decoded.getDataAsArray()));
    }

    @Test
    public void testDecodeH263Packet() {
        byte[] h263packet = ByteUtils.convertHexStringToByteArray(H263_PACKET);
        assertEquals(h263packet.length, 1145);

        DataPacket packet = DataPacket.decode(h263packet);
        assertEquals(RtpVersion.V2, packet.getVersion());
        assertFalse(packet.hasExtension());
        assertTrue(packet.hasMarker());
        assertEquals(4664, packet.getSequenceNumber());
        assertEquals(1248975, packet.getTimestamp());
        assertEquals(0x4fbc4ca1, packet.getSsrc());
        assertEquals(1145 - 12, packet.getDataSize());
        System.err.println(packet);
    }

    @Test
    public void testEncodeDecodeWithFixedBlockSize() {
        DataPacket packet = new DataPacket();
        packet.setMarker(true);
        packet.setSsrc(0x45);
        packet.setSequenceNumber(2);
        packet.setPayloadType(8);
        packet.setTimestamp(69);
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45, 0x45});
        System.out.println("packet = " + packet);

        ChannelBuffer encoded = packet.encode(64);
        System.out.println(ByteUtils.writeArrayAsHex(encoded.array(), true));
        assertEquals(64, encoded.readableBytes());

        DataPacket decoded = DataPacket.decode(encoded);
        assertEquals(0, encoded.readableBytes());

        assertEquals(packet.hasMarker(), decoded.hasMarker());
        assertEquals(packet.getSsrc(), decoded.getSsrc());
        assertEquals(packet.getSequenceNumber(), decoded.getSequenceNumber());
        assertEquals(packet.getPayloadType(), decoded.getPayloadType());
        assertEquals(packet.getTimestamp(), decoded.getTimestamp());
        assertNotNull(decoded.getData());
        assertEquals(packet.getDataSize(), decoded.getDataSize());
        assertTrue(Arrays.equals(packet.getDataAsArray(), decoded.getDataAsArray()));
        System.out.println("decoded = " + decoded);
    }
}
