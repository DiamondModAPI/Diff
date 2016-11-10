package net.earthcomputer.meme.diff;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.earthcomputer.meme.diff.IPatchFileFormat.PatchInfo;
import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

/**
 * This class is used to find the difference between the base file and the work
 * file
 */
public class DiffFinder<T> {

	@SuppressWarnings("unchecked")
	public static <T> void main(String[] args) {
		if (args.length < 3 || args.length > 5) {
			System.err.println(
					"java -jar memediff.jar <base-file> <work-file> <output-patch-file> [patch-format] [patch-file-format]");
			System.exit(1);
			return;
		}

		File baseFile = new File(args[0]);
		if (!baseFile.isFile()) {
			System.err.println("The file \"" + baseFile + "\" does not exist");
			System.exit(1);
			return;
		}

		File workFile = new File(args[1]);
		if (!workFile.isFile()) {
			System.err.println("The file \"" + workFile + "\" does not exist");
			System.exit(1);
			return;
		}

		File patchFile = new File(args[2]);
		if (patchFile.exists()) {
			System.err.println("The file \"" + patchFile + "\" already exists and must be deleted first");
			System.exit(1);
			return;
		}

		IDiffFormat<T> patchFormat = (IDiffFormat<T>) DiffFormats.NORMAL;
		if (args.length > 3) {
			patchFormat = (IDiffFormat<T>) DiffFormats.getByName(args[3]);
			if (patchFormat == null) {
				patchFormat = (IDiffFormat<T>) DiffFormats.NORMAL;
			}
		}

		IPatchFileFormat patchFileFormat = PatchFileFormats.TEXT;
		if (args.length > 4) {
			patchFileFormat = PatchFileFormats.byName(args[4]);
			if (patchFileFormat == null) {
				patchFileFormat = PatchFileFormats.TEXT;
			}
		}

		try {
			new DiffFinder.Builder<T>().setPatchFileFormat(patchFileFormat).setDiffFormat(patchFormat)
					.setBaseFile(baseFile).setWorkFile(workFile).setOutputFile(patchFile).build().writePatchFile();
		} catch (IOException e) {
			System.err.println("An I/O error occurred");
			e.printStackTrace();
			System.exit(1);
			return;
		}
	}

	private final IPatchFileFormat patchFileFormat;
	private final IDiffFormat<T> format;
	private final List<T> baseLines;
	private final List<T> workLines;
	private final OutputStream output;

	private DiffFinder(IPatchFileFormat patchFileFormat, IDiffFormat<T> format, List<T> baseLines, List<T> workLines,
			OutputStream output) {
		this.patchFileFormat = patchFileFormat;
		this.format = format;
		this.baseLines = baseLines;
		this.workLines = workLines;
		this.output = output;
	}

	/**
	 * Computes and writes the patch file
	 */
	public void writePatchFile() throws IOException {
		Patch<T> patch = computePatch();

		patchFileFormat.writePatch(new PatchInfo<T>(format, patch), output);
	}

	/**
	 * Computes the patch and returns it
	 */
	public Patch<T> computePatch() {
		Patch<T> patch = new Patch<T>();
		List<T> lcs = lcs();

		int indexInLcs = 0;
		int indexInBase = 0;
		int indexInWork = 0;

		int currentAdditionStart = -1;
		int currentAdditionLength = -1;
		List<T> currentAddition = new ArrayList<T>();
		int currentDeletionStart = -1;
		int currentDeletionLength = -1;

		while (indexInBase < baseLines.size() || indexInWork < workLines.size()) {
			T lineInLcs = indexInLcs < lcs.size() ? lcs.get(indexInLcs) : null;
			T lineInBase = indexInBase < baseLines.size() ? baseLines.get(indexInBase) : null;
			T lineInWork = indexInWork < workLines.size() ? workLines.get(indexInWork) : null;

			boolean baseEqualsLcs = lineInLcs == null ? lineInBase == null : lineInLcs.equals(lineInBase);
			boolean workEqualsLcs = lineInLcs == null ? lineInWork == null : lineInLcs.equals(lineInWork);

			boolean added = false, deleted = false;

			if (baseEqualsLcs && workEqualsLcs) {
				// No change
				indexInLcs++;
				indexInBase++;
				indexInWork++;
			} else if (baseEqualsLcs) {
				// Addition
				indexInWork++;
				if (currentAdditionStart == -1) {
					currentAdditionStart = indexInBase;
					currentAdditionLength = 0;
				}
				currentAddition.add(lineInWork);
				currentAdditionLength++;
				added = true;
			} else {
				// Deletion
				indexInBase++;
				if (currentDeletionStart == -1) {
					currentDeletionStart = indexInBase;
					currentDeletionLength = 0;
				}
				currentDeletionLength++;
				deleted = true;
			}

			if (!added && currentAdditionStart != -1) {
				// Copy addition to patch
				patch.addAddition(new Addition<T>(currentAddition, currentAdditionStart, currentAdditionLength));
				currentAddition = new ArrayList<T>();
				currentAdditionStart = -1;
				currentAdditionLength = -1;
			}

			if (!deleted && currentDeletionStart != -1) {
				// Copy deletion to patch
				patch.addDeletion(new Deletion<T>(currentDeletionStart, currentDeletionLength));
				currentDeletionStart = -1;
				currentDeletionLength = -1;
			}
		}

		// Flush any non-copied addition and deletion to patch
		if (currentAdditionStart != -1) {
			patch.addAddition(new Addition<T>(currentAddition, currentAdditionStart, currentAdditionLength));
		}

		if (currentDeletionStart != -1) {
			patch.addDeletion(new Deletion<T>(currentDeletionStart, currentDeletionLength));
		}

		return patch;
	}

	private List<T> lcs() {
		int[][] lengths = new int[baseLines.size() + 1][workLines.size() + 1];

		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < baseLines.size(); i++)
			for (int j = 0; j < workLines.size(); j++)
				if (baseLines.get(i).equals(workLines.get(j)))
					lengths[i + 1][j + 1] = lengths[i][j] + 1;
				else
					lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);

		// read the substring out from the matrix
		List<T> lcs = new ArrayList<T>();
		for (int x = baseLines.size(), y = workLines.size(); x != 0 && y != 0;) {
			if (lengths[x][y] == lengths[x - 1][y])
				x--;
			else if (lengths[x][y] == lengths[x][y - 1])
				y--;
			else {
				assert baseLines.get(x - 1).equals(workLines.get(y - 1));
				lcs.add(baseLines.get(x - 1));
				x--;
				y--;
			}
		}
		Collections.reverse(lcs);

		return lcs;
	}

	public static class Builder<T> {
		private IPatchFileFormat patchFileFormat = PatchFileFormats.TEXT;
		private IDiffFormat<T> format;
		private List<T> baseLines;
		private List<T> workLines;
		private OutputStream output;

		public Builder<T> setPatchFileFormat(IPatchFileFormat format) {
			this.patchFileFormat = format;
			return this;
		}

		public Builder<T> setDiffFormat(IDiffFormat<T> format) {
			this.format = format;
			return this;
		}

		public Builder<T> setBaseLines(List<T> baseLines) {
			this.baseLines = baseLines;
			return this;
		}

		public Builder<T> setBaseInputStream(InputStream baseInputStream) {
			try {
				return setBaseLines(format.readElementsFromBaseFile(baseInputStream));
			} catch (IOException e) {
				throw new NoSuchFileException("baseInputStream", e);
			}
		}

		public Builder<T> setBaseFile(File file) {
			return setBaseInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setWorkLines(List<T> workLines) {
			this.workLines = workLines;
			return this;
		}

		public Builder<T> setWorkInputStream(InputStream workInputStream) {
			try {
				return setWorkLines(format.readElementsFromBaseFile(workInputStream));
			} catch (IOException e) {
				throw new NoSuchFileException("workInputStream", e);
			}
		}

		public Builder<T> setWorkFile(File file) {
			return setWorkInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setOutputStream(OutputStream output) {
			this.output = output;
			return this;
		}

		public Builder<T> setOutputFile(File output) {
			return setOutputStream(Utils.getFileOutputStream(output));
		}

		public DiffFinder<T> build() {
			if (patchFileFormat == null) {
				throw new IllegalStateException("patchFileFormat cannot be null");
			}
			if (format == null) {
				throw new IllegalStateException("format not set");
			}
			if (baseLines == null) {
				throw new IllegalStateException("baseLines not set");
			}
			if (workLines == null) {
				throw new IllegalStateException("workLines not set");
			}
			if (output == null) {
				throw new IllegalStateException("output not set");
			}
			return new DiffFinder<T>(patchFileFormat, format, baseLines, workLines, output);
		}
	}

}
