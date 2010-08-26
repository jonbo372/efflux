package org.factor45.efflux.network;

import org.factor45.efflux.packet.RtcpPacket;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
@ChannelHandler.Sharable
public class ControlPacketEncoder extends OneToOneEncoder {

    // constructors ---------------------------------------------------------------------------------------------------

    private ControlPacketEncoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static ControlPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // OneToOneEncoder ------------------------------------------------------------------------------------------------

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof RtcpPacket)) {
            return ChannelBuffers.EMPTY_BUFFER;
        }

        return ChannelBuffers.EMPTY_BUFFER;
        // return ((RtcpPacket) msg).encode();
    }

    // private classes ------------------------------------------------------------------------------------------------

    private static final class InstanceHolder {
        private static final ControlPacketEncoder INSTANCE = new ControlPacketEncoder();
    }
}
