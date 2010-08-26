package org.factor45.efflux;

import org.factor45.efflux.packet.RtpPacket;

import java.net.SocketAddress;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface DataPacketReceiver {

    void dataPacketReceived(RtpPacket packet, SocketAddress origin);
}
