package org.factor45.efflux.network;

import org.factor45.efflux.ControlPacketReceiver;
import org.factor45.efflux.packet.RtcpPacket;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class ControlHandler extends SimpleChannelUpstreamHandler {

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
            this.receiver.controlPacketReceived((RtcpPacket) e.getMessage(), e.getRemoteAddress());
        }
    }
    
    // public methods -------------------------------------------------------------------------------------------------

    public int getPacketsReceived() {
        return this.counter.get();
    }
}
