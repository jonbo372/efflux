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

package org.factor45.efflux.session;

import org.factor45.efflux.network.ControlPacketReceiver;
import org.factor45.efflux.network.DataPacketReceiver;
import org.factor45.efflux.packet.CompoundControlPacket;
import org.factor45.efflux.packet.ControlPacket;
import org.factor45.efflux.packet.DataPacket;
import org.factor45.efflux.participant.RtpParticipant;
import org.factor45.efflux.participant.RtpParticipantInfo;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public interface RtpSession extends DataPacketReceiver, ControlPacketReceiver {

    String getId();

    int getPayloadType();

    boolean init();

    void terminate();

    boolean sendData(byte[] data, long timestamp, boolean marked);

    boolean sendDataPacket(DataPacket packet);

    boolean sendControlPacket(ControlPacket packet);

    boolean sendControlPacket(CompoundControlPacket packet);

    RtpParticipant getLocalParticipant();

    boolean addReceiver(RtpParticipant remoteParticipant);

    boolean removeReceiver(RtpParticipant remoteParticipant);

    RtpParticipant getRemoteParticipant(long ssrsc);

    Map<Long, RtpParticipant> getRemoteParticipants();

    void addDataListener(RtpSessionDataListener listener);

    void removeDataListener(RtpSessionDataListener listener);

    void addControlListener(RtpSessionControlListener listener);

    void removeControlListener(RtpSessionControlListener listener);

    void addEventListener(RtpSessionEventListener listener);

    void removeEventListener(RtpSessionEventListener listener);
}
