package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Random;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class RtpParticipant {

    // constants ------------------------------------------------------------------------------------------------------

    private static final Random RANDOM = new Random(System.nanoTime());

    // internal vars --------------------------------------------------------------------------------------------------

    private SocketAddress dataAddress;
    private SocketAddress controlAddress;
    private long ssrc;
    private String name;
    private String cname;
    private String email;
    private String location;
    private String tool;
    private String note;
    private String priv;

    // constructors ---------------------------------------------------------------------------------------------------

    public RtpParticipant(String host, int dataPort, int controlPort, long ssrc) {
        if ((ssrc < 0) || (ssrc > 0xffffffffl)) {
            throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]");
        }
        if ((dataPort < 0) || (dataPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        if ((controlPort < 0) || (controlPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        this.dataAddress = new InetSocketAddress(host, dataPort);
        this.controlAddress = new InetSocketAddress(host, controlPort);
        this.ssrc = ssrc;
    }

    public RtpParticipant(String host, int dataPort, int controlPort) {
        this(host, dataPort, controlPort, generateNewSsrc());
    }

    private RtpParticipant() {
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static RtpParticipant createFromUnexpectedDataPacket(InetSocketAddress origin, RtpPacket packet) {
        RtpParticipant participant = new RtpParticipant();
        participant.dataAddress = origin;
        participant.controlAddress = new InetSocketAddress(origin.getAddress(), origin.getPort() + 1);
        participant.ssrc = packet.getSsrc();
        return participant;
    }

    /**
     * Randomly generates a new SSRC.
     * <p/>
     * Assuming no other source can obtain the exact same seed (or they're using a different algorithm for the random
     * generation) the probability of collision is roughly 10^-4 when the number of RTP sources is 1000.
     * <a href="http://tools.ietf.org/html/rfc3550#section-8.1">RFC 3550, Section 8.1<a>
     * <p/>
     * In this case, collision odds are slightly bigger because the identifier size will be 31 bits (0x7fffffff,
     * {@link Integer#MAX_VALUE} rather than the full 32 bits.
     *
     * @return A new, random, SSRC identifier.
     */
    public static long generateNewSsrc() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void updateRtpAddress(SocketAddress address) {
        this.dataAddress = address;
    }

    public void updateRtcpAddress(SocketAddress address) {
        this.controlAddress = address;
    }

    public long resolveSsrcConflict(long ssrcToAvoid) {
        // Will hardly ever loop more than once...
        while (this.ssrc == ssrcToAvoid) {
            this.ssrc = generateNewSsrc();
        }

        return this.ssrc;
    }

    public long resolveSsrcConflict(Collection<Long> ssrcsToAvoid) {
        // Probability to execute more than once is higher than the other method that takes just a long as parameter,
        // but its still incredibly low: for 1000 participants, there's roughly 2*10^-7 chance of collision
        while (ssrcsToAvoid.contains(this.ssrc)) {
            this.ssrc = generateNewSsrc();
        }

        return this.ssrc;
    }

    /**
     * USE THIS WITH EXTREME CAUTION at the risk of seriously screwing up the way sessions handle data from incoming
     * participants.
     *
     * @param ssrc The new SSRC.
     */
    public void updateSsrc(long ssrc) {
        if ((ssrc < 0) || (ssrc > 0xffffffffl)) {
            throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]");
        }

        this.ssrc = ssrc;
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public SocketAddress getDataAddress() {
        return dataAddress;
    }

    public SocketAddress getControlAddress() {
        return controlAddress;
    }

    public long getSsrc() {
        return ssrc;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPriv() {
        return priv;
    }

    public void setPriv(String priv) {
        this.priv = priv;
    }

    // low level overrides --------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("RtpParticipant{")
                .append("ssrc=").append(ssrc)
                .append(", dataAddress=").append(dataAddress)
                .append(", controlAddress=").append(controlAddress);

        if (this.name != null) {
            builder.append(", name='").append(name).append('\'');
        }
        if (this.cname != null) {
            builder.append(", cname='").append(cname).append('\'');
        }

        if (this.email != null) {
            builder.append(", email='").append(email).append('\'');
        }

        if (this.location != null) {
            builder.append(", location='").append(location).append('\'');
        }
        if (this.tool != null) {
            builder.append(", tool='").append(tool).append('\'');
        }

        if (this.note != null) {
            builder.append(", note='").append(note).append('\'');
        }

        if (this.priv != null) {
            builder.append(", priv='").append(priv).append('\'');
        }

        return builder.append('}').toString();
    }
}
