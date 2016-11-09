package net.earthcomputer.meme.diff;

import java.util.HashMap;
import java.util.Map;

public class DiffFormats {

	private DiffFormats() {
	}

	public static final NormalDiffFormat NORMAL = new NormalDiffFormat();
	public static final JavaDiffFormat JAVA = new JavaDiffFormat();
	public static final ByteDiffFormat BYTE = new ByteDiffFormat();

	private static final IDiffFormat<?>[] FORMATS = { NORMAL, JAVA, BYTE };
	private static final Map<String, IDiffFormat<?>> nameToFormat = new HashMap<String, IDiffFormat<?>>();

	static {
		for (IDiffFormat<?> format : FORMATS) {
			nameToFormat.put(format.getName(), format);
		}
	}

	public static IDiffFormat<?> getByName(String name) {
		return nameToFormat.get(name);
	}

}
