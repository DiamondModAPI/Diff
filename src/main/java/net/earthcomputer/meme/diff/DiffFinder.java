package net.earthcomputer.meme.diff;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

public class DiffFinder {

	public static final int VERSION = 0;

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("java -jar memediff.jar <base-file> <work-file> <output-patch-file>");
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

		new DiffFinder.Builder().setBaseFile(baseFile).setWorkFile(workFile).setOutputFile(patchFile).build()
				.writePatchFile();
	}

	private final List<String> baseLines;
	private final List<String> workLines;
	private final PrintWriter output;

	private DiffFinder(List<String> baseLines, List<String> workLines, PrintWriter output) {
		this.baseLines = baseLines;
		this.workLines = workLines;
		this.output = output;
	}

	public void writePatchFile() {
		Patch patch = computePatch();

		Iterator<Addition> additionItr = patch.getAdditions().iterator();
		Iterator<Deletion> deletionItr = patch.getDeletions().iterator();
		Addition addition = additionItr.hasNext() ? additionItr.next() : null;
		Deletion deletion = deletionItr.hasNext() ? deletionItr.next() : null;

		output.println("meme-diff version " + VERSION);

		int prevStart = -1;
		while (addition != null || deletion != null) {
			boolean deletionFirst;
			if (addition == null) {
				deletionFirst = true;
			} else if (deletion == null) {
				deletionFirst = false;
			} else {
				deletionFirst = deletion.getStart() <= addition.getStart();
			}

			if (deletionFirst) {
				if (deletion.getStart() != prevStart) {
					output.println();
					prevStart = deletion.getStart();
				}
				output.println("!delete " + deletion.getStart() + "," + deletion.getLength());
				deletion = deletionItr.hasNext() ? deletionItr.next() : null;
			} else {
				if (addition.getStart() != prevStart) {
					output.println();
					prevStart = addition.getStart();
				}
				output.println("!add " + addition.getStart() + "," + addition.getLength());
				for (String addedLine : addition.getAddedLines()) {
					output.println(addedLine);
				}
				addition = additionItr.hasNext() ? additionItr.next() : null;
			}
		}

		output.flush();
	}

	public Patch computePatch() {
		Patch patch = new Patch();
		List<String> lcs = lcs();

		int indexInLcs = 0;
		int indexInBase = 0;
		int indexInWork = 0;

		int currentAdditionStart = -1;
		int currentAdditionLength = -1;
		List<String> currentAddition = new ArrayList<String>();
		int currentDeletionStart = -1;
		int currentDeletionLength = -1;

		while (indexInBase < baseLines.size() || indexInWork < workLines.size()) {
			String lineInLcs = indexInLcs < lcs.size() ? lcs.get(indexInLcs) : null;
			String lineInBase = indexInBase < baseLines.size() ? baseLines.get(indexInBase) : null;
			String lineInWork = indexInWork < workLines.size() ? workLines.get(indexInWork) : null;

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
				patch.addAddition(new Addition(currentAddition, currentAdditionStart, currentAdditionLength));
				currentAddition = new ArrayList<String>();
				currentAdditionStart = -1;
				currentAdditionLength = -1;
			}

			if (!deleted && currentDeletionStart != -1) {
				// Copy deletion to patch
				patch.addDeletion(new Deletion(currentDeletionStart, currentDeletionLength));
				currentDeletionStart = -1;
				currentDeletionLength = -1;
			}
		}

		// Flush any non-copied addition and deletion to patch
		if (currentAdditionStart != -1) {
			patch.addAddition(new Addition(currentAddition, currentAdditionStart, currentAdditionLength));
		}

		if (currentDeletionStart != -1) {
			patch.addDeletion(new Deletion(currentDeletionStart, currentDeletionLength));
		}

		return patch;
	}

	private List<String> lcs() {
		int[][] lengths = new int[baseLines.size() + 1][workLines.size() + 1];

		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < baseLines.size(); i++)
			for (int j = 0; j < workLines.size(); j++)
				if (baseLines.get(i).equals(workLines.get(j)))
					lengths[i + 1][j + 1] = lengths[i][j] + 1;
				else
					lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);

		// read the substring out from the matrix
		List<String> lcs = new ArrayList<String>();
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

	public static class Builder {
		private List<String> baseLines;
		private List<String> workLines;
		private PrintWriter output;

		public Builder setBaseLines(List<String> baseLines) {
			this.baseLines = baseLines;
			return this;
		}

		public Builder setBaseReader(Reader baseReader) {
			return setBaseLines(Utils.getLinesFromReader(baseReader));
		}

		public Builder setBaseInputStream(InputStream baseInputStream) {
			return setBaseReader(new InputStreamReader(baseInputStream));
		}

		public Builder setBaseFile(File file) {
			return setBaseReader(Utils.getFileReader(file));
		}

		public Builder setWorkLines(List<String> workLines) {
			this.workLines = workLines;
			return this;
		}

		public Builder setWorkReader(Reader workReader) {
			return setWorkLines(Utils.getLinesFromReader(workReader));
		}

		public Builder setWorkInputStream(InputStream workInputStream) {
			return setWorkReader(new InputStreamReader(workInputStream));
		}

		public Builder setWorkFile(File file) {
			return setWorkReader(Utils.getFileReader(file));
		}

		public Builder setOutput(PrintWriter output) {
			this.output = output;
			return this;
		}

		public Builder setOutputWriter(Writer writer) {
			return setOutput(new PrintWriter(writer));
		}

		public Builder setOutputStream(OutputStream output) {
			return setOutput(new PrintWriter(output));
		}

		public Builder setOutputFile(File output) {
			return setOutputWriter(Utils.getFileWriter(output));
		}

		public DiffFinder build() {
			if (baseLines == null) {
				throw new IllegalStateException("baseLines not set");
			}
			if (workLines == null) {
				throw new IllegalStateException("workLines not set");
			}
			if (output == null) {
				throw new IllegalStateException("output not set");
			}
			return new DiffFinder(baseLines, workLines, output);
		}
	}

}
