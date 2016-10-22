package net.earthcomputer.diamond.diff;

public class InvalidPatchFormatException extends Exception {

	private static final long serialVersionUID = 7137821036526817522L;

	public InvalidPatchFormatException(String desc, Throwable cause) {
		super(desc, cause);
	}

	public InvalidPatchFormatException(String desc) {
		super(desc);
	}

}
