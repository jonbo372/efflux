package org.factor45.efflux.session;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class RtpParticipantContext {

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipant participant;

    // internal vars --------------------------------------------------------------------------------------------------

    private long byeReceptionInstant;
    private int lastSequenceNumber;

    // constructors ---------------------------------------------------------------------------------------------------

    public RtpParticipantContext(RtpParticipant participant) {
        this.participant = participant;

        this.lastSequenceNumber = -1;
        this.byeReceptionInstant = -1;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void byeReceived() {
        this.byeReceptionInstant = System.currentTimeMillis();
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public RtpParticipant getParticipant() {
        return participant;
    }

    public long getByeReceptionInstant() {
        return byeReceptionInstant;
    }

    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setLastSequenceNumber(int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }
}
