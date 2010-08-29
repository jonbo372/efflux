package org.factor45.efflux.network;

import org.factor45.efflux.logging.Logger;
import org.factor45.efflux.packet.RtpPacket;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class DataHandler extends SimpleChannelUpstreamHandler {

    // constants ------------------------------------------------------------------------------------------------------

    private static final Logger LOG = Logger.getLogger(DataHandler.class);

    // internal vars --------------------------------------------------------------------------------------------------

    private final AtomicInteger counter;
    private final DataPacketReceiver receiver;

    // constructors ---------------------------------------------------------------------------------------------------

    public DataHandler(DataPacketReceiver receiver) {
        this.receiver = receiver;
        this.counter = new AtomicInteger();
    }

    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof RtpPacket) {
            this.receiver.dataPacketReceived(e.getRemoteAddress(), (RtpPacket) e.getMessage());
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
