package net.earthcomputer.meme.diff;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains static references to all the diff formats
 */
public class DiffFormats {

	private DiffFormats() {
	}

	/**
	 * The normal diff format
	 */
	public static final NormalDiffFormat NORMAL = new NormalDiffFormat();
	/**
	 * The java diff format
	 */
	public static final JavaDiffFormat JAVA = new JavaDiffFormat();
	/**
	 * The byte diff format
	 */
	public static final ByteDiffFormat BYTE = new ByteDiffFormat();

	private static final IDiffFormat<?>[] FORMATS = { NORMAL, JAVA, BYTE };
	private static final Map<String, IDiffFormat<?>> nameToFormat = new HashMap<String, IDiffFormat<?>>();

	static {
		for (IDiffFormat<?> format : FORMATS) {
			nameToFormat.put(format.getName(), format);
		}
	}

	/**
	 * Gets a diff format by its name
	 */
	public static IDiffFormat<?> getByName(String name) {
		return nameToFormat.get(name);
	}

}
