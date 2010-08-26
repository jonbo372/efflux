package org.factor45.efflux.packet;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class RtcpPacket {

    // public classes -------------------------------------------------------------------------------------------------

    public static enum Type {
        SENDER_REPORT,
        RECEIVER_REPORT,
        SOURCE_DESCRIPTION,
        BYE,
        APP_DATA
    }
}
