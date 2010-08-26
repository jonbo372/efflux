package org.factor45.efflux;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public class Participant {

    // internal vars --------------------------------------------------------------------------------------------------

    private long synchronisationSourceId;
    private String cname;
    private SocketAddress rtpAddress;
    private SocketAddress rtcpAddress;

    private String name;
    private String email;
    private String location;
    private String tool;
    private String note;
    private String priv;

    private long byeReceptionInstant;

    // constructors ---------------------------------------------------------------------------------------------------

    public Participant(String remoteHost, int rtpPort, int rtcpPort) {
        this.rtpAddress = new InetSocketAddress(remoteHost, rtpPort);
        this.rtcpAddress = new InetSocketAddress(remoteHost, rtcpPort);
        this.byeReceptionInstant = -1;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void updateRtpAddress(SocketAddress address) {
        this.rtpAddress = address;
    }

    public void updateRtcpAddress(SocketAddress address) {
        this.rtcpAddress = address;
    }

    public void byeReceived() {
        this.byeReceptionInstant = System.currentTimeMillis();
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public SocketAddress getRtpAddress() {
        return rtpAddress;
    }

    public SocketAddress getRtcpAddress() {
        return rtcpAddress;
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

    public long getByeReceptionInstant() {
        return byeReceptionInstant;
    }
}
