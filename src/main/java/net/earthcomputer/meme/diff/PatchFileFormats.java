package net.earthcomputer.meme.diff;

import java.util.HashMap;
import java.util.Map;

public class PatchFileFormats {

	private PatchFileFormats() {
	}

	public static final TextPatchFileFormat TEXT = new TextPatchFileFormat();
	public static final BinaryPatchFileFormat BINARY = new BinaryPatchFileFormat();

	private static final IPatchFileFormat[] FORMATS = { TEXT, BINARY };
	private static final Map<String, IPatchFileFormat> FORMATS_BY_NAME = new HashMap<String, IPatchFileFormat>();

	static {
		for (IPatchFileFormat format : FORMATS) {
			FORMATS_BY_NAME.put(format.getName(), format);
		}
	}

	public static IPatchFileFormat byName(String name) {
		return FORMATS_BY_NAME.get(name);
	}

}
