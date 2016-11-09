package net.earthcomputer.meme.diff;

import java.util.HashMap;
import java.util.Map;

public class DiffFormats {

	private DiffFormats() {
	}

	public static final NormalDiffFormat NORMAL = new NormalDiffFormat();
	public static final JavaDiffFormat JAVA = new JavaDiffFormat();

	private static final IDiffFormat<?>[] FORMATS = { NORMAL, JAVA };
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
