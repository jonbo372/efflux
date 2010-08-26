package org.factor45.efflux;

import org.factor45.efflux.packet.RtpPacket;
import org.factor45.efflux.packet.RtpVersion;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class RtpPacketTest {

    public static final byte[] ALAW_RTP_PACKET_SAMPLE =
            {(byte) 0x80, (byte) 0x88, 0x19, 0x73, 0x00, 0x01, (byte) 0x95, 0x14, 0x1f, (byte) 0xcc, 0x77,
                       (byte) 0x9a, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5};

    @Test
    public void testDecode() {
        RtpPacket packet = RtpPacket.decode(ALAW_RTP_PACKET_SAMPLE);
        assertEquals(RtpVersion.V2, packet.getVersion());
        assertFalse(packet.hasPadding());
        assertFalse(packet.hasExtension());
        assertEquals(0, packet.getContributingSourcesCount());
        assertTrue(packet.hasMarker());
        assertEquals(8, packet.getPayloadType());
        assertEquals(6515, packet.getSequenceNumber());
        assertEquals(103700, packet.getTimestamp());
        assertEquals(0x1fcc779a, packet.getSynchronisationSourceId());
        assertEquals(6, packet.getDataSize());
    }

    @Test
    public void testEncode() {
        RtpPacket packet = new RtpPacket();
        packet.setVersion(RtpVersion.V2);
        packet.setMarker(true);
        packet.setPayloadType(8);
        packet.setSequenceNumber(6515);
        packet.setTimestamp(103700);
        packet.setSynchronisationSourceId(0x1fcc779a);
        packet.setData(new byte[]{(byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5, (byte) 0xd5});
        ChannelBuffer buffer = packet.encode();
        assertTrue(Arrays.equals(ALAW_RTP_PACKET_SAMPLE, buffer.array()));
    }

    @Test
    public void testEncodeDecode() {
        RtpPacket packet = new RtpPacket();
        packet.setVersion(RtpVersion.V2);
        packet.setMarker(true);
        packet.setPayloadType(98);
        packet.setPadding(true);
        packet.setSequenceNumber(69);
        packet.setTimestamp(696969);
        packet.setSynchronisationSourceId(96);
        packet.setExtensionHeader((short) 0x8080, new byte[]{0x70, 0x70, 0x70, 0x70});
        packet.addContributingSourceId(69);
        packet.addContributingSourceId(70);
        packet.addContributingSourceId(71);
        packet.setData(new byte[]{0x69, 0x69, 0x69, 0x69});

        ChannelBuffer buffer = packet.encode();

        RtpPacket decoded = RtpPacket.decode(buffer);
        assertEquals(packet.getVersion(), decoded.getVersion());
        assertEquals(packet.hasMarker(), decoded.hasMarker());
        assertEquals(packet.getPayloadType(), decoded.getPayloadType());
        assertEquals(packet.getSequenceNumber(), decoded.getSequenceNumber());
        assertEquals(packet.getTimestamp(), decoded.getTimestamp());
        assertEquals(packet.getSynchronisationSourceId(), decoded.getSynchronisationSourceId());
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
}
