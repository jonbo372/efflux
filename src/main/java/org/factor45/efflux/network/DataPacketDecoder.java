package org.factor45.efflux.network;

import org.factor45.efflux.packet.RtpPacket;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class DataPacketDecoder extends OneToOneDecoder {

    // constants ------------------------------------------------------------------------------------------------------

    protected static final InternalLogger LOG = InternalLoggerFactory.getInstance(OneToOneDecoder.class);

    // OneToOneDecoder ------------------------------------------------------------------------------------------------

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof ChannelBuffer)) {
            return null;
        }

        try {
            return RtpPacket.decode((ChannelBuffer) msg);
        } catch (Exception e) {
            LOG.debug("Failed to decode RTP packet.", e);
            return null;
        }
    }
}
