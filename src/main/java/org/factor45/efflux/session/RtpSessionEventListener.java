package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;

import java.net.SocketAddress;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface RtpSessionEventListener {

    void participantJoinedFromData(RtpSession session, RtpParticipant participant, RtpPacket packet);

    void participantJoinedFromControl(RtpSession session, RtpParticipant participant, RtpPacket packet);

    void participantLeft(RtpSession session, RtpParticipant participant);

    void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc);
}
