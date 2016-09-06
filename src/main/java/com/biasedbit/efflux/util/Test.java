/**
 * com.biasedbit.efflux.util
 * PersistenceProcessor.java
 * 2016/8/29
 * Copyright (c) Genew Technologies 2010-2010. All rights reserved.
 */
package com.biasedbit.efflux.util;

import com.biasedbit.efflux.packet.AppDataPacket;
import com.biasedbit.efflux.packet.CompoundControlPacket;
import com.biasedbit.efflux.packet.ControlPacket;
import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.*;

import java.util.List;

/**
 * TODO Add class comment here<p/>
 * @version 1.0.0
 * @since 1.0.0
 * @author xiongqimin
 * @history<br/>
 * ver    date       author desc
 * 1.0.0  2016/8/29 xiongqimin created<br/>
 * <p/>
 */
public class Test
{
    public static void main(String[] args)
    {
        RtpParticipant participant = RtpParticipant.createReceiver("10.8.9.64", 2428, 2429);
        MultiParticipantSession session = new MultiParticipantSession("id", 5, participant);
        session.setAutomatedRtcpHandling(false);
        session.init();

        RtpParticipant receiver1 = RtpParticipant.createReceiver("10.8.9.194", 22224,22225);
        session.addReceiver(receiver1);

        session.addControlListener(new RtpSessionControlListener()
        {
            @Override
            public void controlPacketReceived(RtpSession session, CompoundControlPacket packet)
            {
                System.out.println("controlPacketReceived");
//                session.addReceiver()
                session.sendControlPacket(packet);
            }

            @Override
            public void appDataReceived(RtpSession session, AppDataPacket appDataPacket)
            {
                System.out.println("appDataReceived");
            }
        });
        session.addDataListener(new RtpSessionDataListener()
        {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet)
            {
                System.out.println("dataPacketReceived");
                session.sendDataPacket(packet);
            }
        });
    }

    private static class RtpDestPair
    {

    }
}
