package net.earthcomputer.meme.diff;

/**
 * Thrown when the input patch file has invalid format
 */
public class InvalidPatchFormatException extends Exception {

	private static final long serialVersionUID = 7137821036526817522L;

	public InvalidPatchFormatException(String desc, Throwable cause) {
		super(desc, cause);
	}

	public InvalidPatchFormatException(String desc) {
		super(desc);
	}

}
