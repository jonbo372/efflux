package org.factor45.efflux.session;

import org.factor45.efflux.packet.RtpPacket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class RtpParticipant {

    // internal vars --------------------------------------------------------------------------------------------------

    private SocketAddress dataAddress;
    private SocketAddress controlAddress;
    private long synchronisationSourceId;
    private String name;
    private String cname;
    private String email;
    private String location;
    private String tool;
    private String note;
    private String priv;

    // constructors ---------------------------------------------------------------------------------------------------

    public RtpParticipant(String remoteHost, int dataPort, int controlPort, long synchronisationSourceId) {
        if (synchronisationSourceId > 0xffffffffl) {
            throw new IllegalArgumentException("Highest value for SSRC is 0xffffffff");
        }
        if ((dataPort < 0) || (dataPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        if ((controlPort < 0) || (controlPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        this.dataAddress = new InetSocketAddress(remoteHost, dataPort);
        this.controlAddress = new InetSocketAddress(remoteHost, controlPort);
        this.synchronisationSourceId = synchronisationSourceId;
    }

    private RtpParticipant() {
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static RtpParticipant createFromUnexpectedDataPacket(InetSocketAddress origin, RtpPacket packet) {
        RtpParticipant participant = new RtpParticipant();
        participant.dataAddress = origin;
        participant.controlAddress = new InetSocketAddress(origin.getAddress(), origin.getPort() + 1);
        participant.synchronisationSourceId = packet.getSynchronisationSourceId();
        return participant;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void updateRtpAddress(SocketAddress address) {
        this.dataAddress = address;
    }

    public void updateRtcpAddress(SocketAddress address) {
        this.controlAddress = address;
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public SocketAddress getDataAddress() {
        return dataAddress;
    }

    public SocketAddress getControlAddress() {
        return controlAddress;
    }

    public long getSynchronisationSourceId() {
        return synchronisationSourceId;
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
                .append("ssrc=").append(synchronisationSourceId)
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
