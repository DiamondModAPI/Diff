package net.earthcomputer.meme.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A patch file format, defines how a patch can be represented in a file
 */
public interface IPatchFileFormat {

	/**
	 * Returns the name of this patch file format
	 */
	String getName();

	/**
	 * Reads a patch from the given input stream
	 */
	<T> PatchInfo<T> readPatch(InputStream in) throws InvalidPatchFormatException, IOException;

	/**
	 * Writes a patch to the given output stream
	 */
	<T> void writePatch(PatchInfo<T> patch, OutputStream out) throws IOException;

	/**
	 * Contains the information that should be stored in a patch file
	 */
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
