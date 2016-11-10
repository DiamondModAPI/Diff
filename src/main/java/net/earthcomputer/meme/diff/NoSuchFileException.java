package net.earthcomputer.meme.diff;

/**
 * Thrown when an input file doesn't exist or is unable to be read from
 */
public class NoSuchFileException extends RuntimeException {

	private static final long serialVersionUID = -1737780871009448973L;

	public NoSuchFileException(String desc, Throwable cause) {
		super(desc, cause);
	}

	public NoSuchFileException(String desc) {
		super(desc);
	}

}
