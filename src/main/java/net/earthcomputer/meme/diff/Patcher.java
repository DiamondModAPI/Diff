package net.earthcomputer.meme.diff;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.earthcomputer.meme.diff.IPatchFileFormat.PatchInfo;
import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

public class Patcher<T> {

	public static void main(String[] args) {
		if (args.length < 3 || args.length > 4) {
			System.err.println(
					"java -cp memediff.jar net.earthcomputer.meme.diff.Patcher <base-file> <patch-file> <output-work-file> [patch-file-format]");
			System.exit(1);
			return;
		}

		File baseFile = new File(args[0]);
		if (!baseFile.isFile()) {
			System.err.println("The file \"" + baseFile + "\" does not exist");
			System.exit(1);
			return;
		}

		File patchFile = new File(args[1]);
		if (!patchFile.isFile()) {
			System.err.println("The file \"" + patchFile + "\" does not exist");
			System.exit(1);
			return;
		}

		File workFile = new File(args[2]);
		if (workFile.exists()) {
			System.err.println("The file \"" + workFile + "\" already exists and must be deleted first");
			System.exit(1);
			return;
		}

		IPatchFileFormat patchFileFormat = PatchFileFormats.TEXT;
		if (args.length > 3) {
			patchFileFormat = PatchFileFormats.byName(args[3]);
			if (patchFileFormat == null) {
				patchFileFormat = PatchFileFormats.TEXT;
			}
		}

		try {
			new Patcher.Builder<Object>().setPatchFileFormat(patchFileFormat).setPatchFile(patchFile)
					.setBaseFile(baseFile).setOutputFile(workFile).build().writeWorkFile();
		} catch (InvalidPatchFormatException e) {
			System.err.println("The patch file had invalid format: " + e.getMessage());
			System.exit(1);
			return;
		}
	}

	private final IDiffFormat<T> format;
	private final List<T> baseLines;
	private final Patch<T> patch;
	private final OutputStream output;

	private Patcher(IDiffFormat<T> format, List<T> baseLines, Patch<T> patch, OutputStream output) {
		this.format = format;
		this.baseLines = baseLines;
		this.patch = patch;
		this.output = output;
	}

	public void writeWorkFile() {
		List<T> workLines = computeWorkFile();

		try {
			format.writeElementsToWorkFile(workLines, output);

			output.flush();
			output.close();
		} catch (IOException e) {
			throw new NoSuchFileException("output", e);
		}
	}

	public List<T> computeWorkFile() {
		List<T> workLines = new ArrayList<T>(baseLines);

		List<Addition<T>> additions = patch.getAdditions();
		List<Deletion<T>> deletions = patch.getDeletions();
		int additionIndex = additions.size() - 1;
		int deletionIndex = deletions.size() - 1;
		Deletion<T> lastDeletion = new Deletion<T>(-1, -1);
		while (additionIndex >= 0 || deletionIndex >= 0) {
			Addition<T> addition = additionIndex >= 0 ? additions.get(additionIndex) : null;
			Deletion<T> deletion = deletionIndex >= 0 ? deletions.get(deletionIndex) : null;
			boolean deletionFirst;
			if (addition == null) {
				deletionFirst = true;
			} else if (deletion == null) {
				deletionFirst = false;
			} else {
				deletionFirst = deletion.getStart() >= addition.getStart();
			}

			if (deletionFirst) {
				int start = deletion.getStart();
				int length = deletion.getLength();
				for (int i = 0; i < length; i++) {
					workLines.remove(start - 1);
				}
				deletionIndex--;
				lastDeletion = deletion;
			} else {
				int start = addition.getStart();
				if (start == lastDeletion.getStart()) {
					start -= lastDeletion.getLength();
				}
				workLines.addAll(start, addition.getAddedLines());
				additionIndex--;
			}
		}
		return workLines;
	}

	public static class Builder<T> {
		private IPatchFileFormat patchFileFormat = PatchFileFormats.TEXT;
		private IDiffFormat<T> format;
		private List<T> baseLines;
		private Patch<T> patch;
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

		public Builder<T> setBaseInputStream(InputStream inputStream) {
			if (format == null) {
				throw new IllegalStateException("Cannot read base file before setting format");
			}
			try {
				return setBaseLines(format.readElementsFromBaseFile(inputStream));
			} catch (IOException e) {
				throw new NoSuchFileException("inputStream", e);
			}
		}

		public Builder<T> setBaseFile(File file) {
			return setBaseInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setPatch(Patch<T> patch) {
			this.patch = patch;
			return this;
		}

		public Builder<T> setPatchInputStream(InputStream inputStream) throws InvalidPatchFormatException {
			if (patchFileFormat == null) {
				throw new IllegalStateException("patchFileFormat cannot be null");
			}
			try {
				PatchInfo<T> info = patchFileFormat.readPatch(inputStream);
				return setDiffFormat(info.getFormat()).setPatch(info.getPatch());
			} catch (IOException e) {
				throw new NoSuchFileException("inputStream", e);
			}
		}

		public Builder<T> setPatchFile(File file) throws InvalidPatchFormatException {
			return setPatchInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setOutput(OutputStream outputStream) {
			this.output = outputStream;
			return this;
		}

		public Builder<T> setOutputFile(File outputFile) {
			return setOutput(Utils.getFileOutputStream(outputFile));
		}

		public Patcher<T> build() {
			if (format == null) {
				throw new IllegalStateException("format == null");
			}
			if (baseLines == null) {
				throw new IllegalStateException("baseLines == null");
			}
			if (patch == null) {
				throw new IllegalStateException("patch == null");
			}
			if (output == null) {
				throw new IllegalStateException("output == null");
			}
			return new Patcher<T>(format, baseLines, patch, output);
		}
	}

}
