package org.factor45.efflux;

import org.factor45.efflux.packet.RtpPacket;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface RtpSessionListener {

    void dataPacketReceived(RtpSession session, RtpPacket packet);
}
