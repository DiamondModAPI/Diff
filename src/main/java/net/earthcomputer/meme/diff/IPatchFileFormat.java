package net.earthcomputer.meme.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IPatchFileFormat {
	
	String getName();

	<T> PatchInfo<T> readPatch(InputStream in) throws InvalidPatchFormatException, IOException;

	<T> void writePatch(PatchInfo<T> patch, OutputStream out) throws IOException;

	public static class PatchInfo<T> {
		private IDiffFormat<T> format;
		private Patch<T> patch;

		public PatchInfo() {
		}

		public PatchInfo(IDiffFormat<T> format, Patch<T> patch) {
			this.format = format;
			this.patch = patch;
		}

		public void setFormat(IDiffFormat<T> format) {
			this.format = format;
		}

		public void setPatch(Patch<T> patch) {
			this.patch = patch;
		}

		public IDiffFormat<T> getFormat() {
			return format;
		}

		public Patch<T> getPatch() {
			return patch;
		}
	}

}
