package org.factor45.efflux.packet;

/**
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 */
public enum RtpVersion {

    // constants ------------------------------------------------------------------------------------------------------

    V2((byte) 0x80),
    V1((byte) 0x40),
    V0((byte) 0x00);

    // internal vars --------------------------------------------------------------------------------------------------

    private final byte b;

    // constructors ---------------------------------------------------------------------------------------------------

    private RtpVersion(byte b) {
        this.b = b;
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static RtpVersion fromByte(byte b) throws IllegalArgumentException {
        byte tmp = (byte) (b & 0xc0);
        // Starts from version 2, which is the most common.
        for (RtpVersion version : values()) {
            if (version.getByte() == tmp) {
                return version;
            }
        }

        throw new IllegalArgumentException("Unknown version for byte: " + b);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public byte getByte() {
        return b;
    }
}
