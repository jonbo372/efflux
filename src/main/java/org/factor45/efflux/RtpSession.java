package org.factor45.efflux;

import org.factor45.efflux.packet.RtcpPacket;
import org.factor45.efflux.packet.RtpPacket;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface RtpSession extends DataPacketReceiver, ControlPacketReceiver {

    String getId();

    boolean init();

    void terminate();

    boolean sendDataPacket(RtpPacket packet);

    boolean sendControlPacket(RtcpPacket packet);

    void addListener(RtpSessionListener listener);

    void removeListener(RtpSessionListener listener);
}
