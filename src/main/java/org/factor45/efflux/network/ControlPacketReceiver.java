package org.factor45.efflux.network;

import org.factor45.efflux.packet.RtcpPacket;

import java.net.SocketAddress;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface ControlPacketReceiver {

    void controlPacketReceived(SocketAddress origin, RtcpPacket packet);
}
