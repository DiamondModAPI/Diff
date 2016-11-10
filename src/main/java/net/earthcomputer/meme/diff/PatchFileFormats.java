package net.earthcomputer.meme.diff;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains static references to all the patch file formats
 */
public class PatchFileFormats {

	private PatchFileFormats() {
	}

	/**
	 * The text-based patch file format
	 */
	public static final TextPatchFileFormat TEXT = new TextPatchFileFormat();
	/**
	 * The binary patch file format
	 */
	public static final BinaryPatchFileFormat BINARY = new BinaryPatchFileFormat();

	private static final IPatchFileFormat[] FORMATS = { TEXT, BINARY };
	private static final Map<String, IPatchFileFormat> FORMATS_BY_NAME = new HashMap<String, IPatchFileFormat>();

	static {
		for (IPatchFileFormat format : FORMATS) {
			FORMATS_BY_NAME.put(format.getName(), format);
		}
	}

	/**
	 * Gets a patch file format by its name
	 */
	public static IPatchFileFormat byName(String name) {
		return FORMATS_BY_NAME.get(name);
	}

}
