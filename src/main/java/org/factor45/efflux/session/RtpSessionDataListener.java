package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface RtpSessionDataListener {

    void dataPacketReceived(RtpSession session, RtpParticipant participant, RtpPacket packet);
}
