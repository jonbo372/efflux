package org.factor45.efflux.network;

import org.factor45.efflux.logging.Logger;
import org.factor45.efflux.packet.RtcpPacket;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class ControlHandler extends SimpleChannelUpstreamHandler {

    // constants ------------------------------------------------------------------------------------------------------

    private static final Logger LOG = Logger.getLogger(ControlHandler.class);

    // internal vars --------------------------------------------------------------------------------------------------

    private final AtomicInteger counter;
    private final ControlPacketReceiver receiver;

    // constructors ---------------------------------------------------------------------------------------------------

    public ControlHandler(ControlPacketReceiver receiver) {
        this.receiver = receiver;
        this.counter = new AtomicInteger();
    }

    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof RtcpPacket) {
            this.receiver.controlPacketReceived(e.getRemoteAddress(), (RtcpPacket) e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        // Just log and proceed...
        LOG.error("Caught exception on channel {}.", e.getCause(), e.getChannel());
    }
    
    // public methods -------------------------------------------------------------------------------------------------

    public int getPacketsReceived() {
        return this.counter.get();
    }
}
