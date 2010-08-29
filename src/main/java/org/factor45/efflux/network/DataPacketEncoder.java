package org.factor45.efflux.network;

import org.factor45.efflux.packet.RtpPacket;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
@ChannelHandler.Sharable
public class DataPacketEncoder extends OneToOneEncoder {

    // constructors ---------------------------------------------------------------------------------------------------

    private DataPacketEncoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static DataPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // OneToOneEncoder ------------------------------------------------------------------------------------------------

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof RtpPacket)) {
            return ChannelBuffers.EMPTY_BUFFER;
        }

        RtpPacket packet = (RtpPacket) msg;
        if (packet.getDataSize() == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }
        return packet.encode();
    }

    // private classes ------------------------------------------------------------------------------------------------

    private static final class InstanceHolder {
        private static final DataPacketEncoder INSTANCE = new DataPacketEncoder();
    }
}
