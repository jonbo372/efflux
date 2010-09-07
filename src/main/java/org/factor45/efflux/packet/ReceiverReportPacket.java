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

package org.factor45.efflux.packet;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class ReceiverReportPacket extends ControlPacket {

    // internal vars --------------------------------------------------------------------------------------------------

    private long senderSsrc;
    private List<ReceptionReport> receptionReports;

    // constructors ---------------------------------------------------------------------------------------------------

    public ReceiverReportPacket() {
        super(Type.RECEIVER_REPORT);
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static ReceiverReportPacket decode(ChannelBuffer buffer, boolean hasPadding, byte innerBlocks, int length) {
        ReceiverReportPacket packet = new ReceiverReportPacket();

        packet.setSenderSsrc(buffer.readUnsignedInt());

        int read = 4;
        for (int i = 0; i < innerBlocks; i++) {
            packet.addSenderReport(ReceptionReport.decode(buffer));
            read += 24; // Each SR/RR block has 24 bytes (6 32bit words)
        }

        // Length is written in 32bit words, not octet count.
        int lengthInOctets = (length * 4);
        // (hasPadding == true) check is not done here. RFC respecting implementations will set the padding bit to 1
        // if length of packet is bigger than the necessary to convey the data; therefore it's a redundant check.
        if (read < lengthInOctets) {
            // Skip remaining bytes (used for padding).
            buffer.skipBytes(lengthInOctets - read);
        }

        return packet;
    }

    // ControlPacket --------------------------------------------------------------------------------------------------

    @Override
    public ChannelBuffer encode(int currentCompoundLength, int fixedBlockSize) {
        return null;
    }

    @Override
    public ChannelBuffer encode() {
        return null;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public boolean addSenderReport(ReceptionReport block) {
        if (this.receptionReports == null) {
            this.receptionReports = new ArrayList<ReceptionReport>();
            return this.receptionReports.add(block);
        }

        // 5 bits is the limit
        return (this.receptionReports.size() < 31) && this.receptionReports.add(block);
    }

    public byte getReceptionReportCount() {
        if (this.receptionReports == null) {
            return 0;
        }

        return (byte) this.receptionReports.size();
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public long getSenderSsrc() {
        return senderSsrc;
    }

    public void setSenderSsrc(long senderSsrc) {
        if ((senderSsrc < 0) || (senderSsrc > 0xffffffffL)) {
            throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]");
        }
        this.senderSsrc = senderSsrc;
    }

    public List<ReceptionReport> getReceptionReports() {
        if (this.receptionReports == null) {
            return null;
        }
        return Collections.unmodifiableList(this.receptionReports);
    }

    public void setReceptionReports(List<ReceptionReport> receptionReports) {
        if (receptionReports.size() >= 31) {
            throw new IllegalArgumentException("At most 31 report blocks can be sent in a ReceiverReportPacket");
        }
        this.receptionReports = receptionReports;
    }

    // low level overrides --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ReceiverReportPacket{")
                .append("senderSsrc=").append(this.senderSsrc)
                .append(", receptionReports=").append(this.receptionReports)
                .append('}').toString();
    }
}
