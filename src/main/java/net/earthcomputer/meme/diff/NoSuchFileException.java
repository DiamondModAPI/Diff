package net.earthcomputer.meme.diff;

public class NoSuchFileException extends RuntimeException {

	private static final long serialVersionUID = -1737780871009448973L;

	public NoSuchFileException(String desc, Throwable cause) {
		super(desc, cause);
	}

	public NoSuchFileException(String desc) {
		super(desc);
	}

}
