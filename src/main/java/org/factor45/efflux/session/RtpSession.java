package org.factor45.efflux.session;

import org.factor45.efflux.network.ControlPacketReceiver;
import org.factor45.efflux.network.DataPacketReceiver;
import org.factor45.efflux.packet.RtcpPacket;
import org.factor45.efflux.packet.RtpPacket;

import java.util.Collection;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public interface RtpSession extends DataPacketReceiver, ControlPacketReceiver {

    String getId();

    int getPayloadType();

    boolean init();

    void terminate();

    boolean sendData(byte[] data, long timestamp);

    boolean sendDataPacket(RtpPacket packet);

    boolean sendControlPacket(RtcpPacket packet);

    boolean addParticipant(RtpParticipant remoteParticipant);

    RtpParticipant removeParticipant(long ssrc);

    RtpParticipant getLocalParticipant();

    RtpParticipant getRemoteParticipant(long ssrsc);

    Collection<RtpParticipant> getRemoteParticipants();

    void addDataListener(RtpSessionDataListener listener);

    void removeDataListener(RtpSessionDataListener listener);

    void addEventListener(RtpSessionEventListener listener);

    void removeEventListener(RtpSessionEventListener listener);
}
