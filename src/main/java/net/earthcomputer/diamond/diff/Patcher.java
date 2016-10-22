package net.earthcomputer.diamond.diff;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.earthcomputer.diamond.diff.Patch.Addition;
import net.earthcomputer.diamond.diff.Patch.Deletion;

public class Patcher {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println(
					"java -cp diamonddiff.jar net.earthcomputer.diamond.diff.Patcher <base-file> <patch-file> <output-work-file>");
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

		try {
			new Patcher.Builder().setBaseFile(baseFile).setPatchFile(patchFile).setOutputFile(workFile).build()
					.writeWorkFile();
		} catch (InvalidPatchFormatException e) {
			System.err.println("The patch file had invalid format: " + e.getMessage());
			System.exit(1);
			return;
		}
	}

	private final List<String> baseLines;
	private final Patch patch;
	private final PrintWriter output;

	private static final Pattern HEADER_PATTERN = Pattern.compile("\\s*diamond-diff\\s+version\\s+(\\d+)\\s*");
	private static final Pattern DELETION_PATTERN = Pattern.compile("\\s*!delete\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");
	private static final Pattern ADDITION_PATTERN = Pattern.compile("\\s*!add\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");

	private Patcher(List<String> baseLines, Patch patch, PrintWriter output) {
		this.baseLines = baseLines;
		this.patch = patch;
		this.output = output;
	}

	public void writeWorkFile() {
		List<String> workLines = computeWorkFile();

		for (int i = 0; i < workLines.size(); i++) {
			String line = workLines.get(i);
			if (i != workLines.size() - 1) {
				output.println(line);
			} else {
				output.print(line);
			}
		}

		output.flush();
	}

	public List<String> computeWorkFile() {
		List<String> workLines = new ArrayList<String>(baseLines);

		List<Addition> additions = patch.getAdditions();
		List<Deletion> deletions = patch.getDeletions();
		int additionIndex = additions.size() - 1;
		int deletionIndex = deletions.size() - 1;
		Deletion lastDeletion = new Deletion(-1, -1);
		while (additionIndex >= 0 || deletionIndex >= 0) {
			Addition addition = additionIndex >= 0 ? additions.get(additionIndex) : null;
			Deletion deletion = deletionIndex >= 0 ? deletions.get(deletionIndex) : null;
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

	public static Patch readPatch(Reader reader) throws InvalidPatchFormatException {
		Patch patch = new Patch();
		List<String> lines = Utils.getLinesFromReader(reader);

		try {
			Matcher headerMatcher = HEADER_PATTERN.matcher(lines.get(0));
			if (!headerMatcher.matches()) {
				throw new InvalidPatchFormatException("Invalid header \"" + lines.get(0) + "\"");
			}
			int patchVersion = Integer.parseInt(headerMatcher.group(1));

			switch (patchVersion) {
			case 0:
				return readPatchVersion0(patch, lines);
			default:
				throw new InvalidPatchFormatException("Unsupported patch file version: " + patchVersion);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new InvalidPatchFormatException("Reached the end of the file unexpectedly");
		}
	}

	private static Patch readPatchVersion0(Patch patch, List<String> lines) {
		for (int index = 1; index < lines.size(); index++) {
			String line = lines.get(index);
			Matcher matcher = DELETION_PATTERN.matcher(line);
			if (matcher.matches()) {
				patch.addDeletion(new Deletion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
			} else {
				matcher = ADDITION_PATTERN.matcher(line);
				if (matcher.matches()) {
					int additionLength = Integer.parseInt(matcher.group(2));

					List<String> addedLines = new ArrayList<String>(additionLength);
					for (int i = 0; i < additionLength; i++) {
						addedLines.add(lines.get(++index));
					}

					patch.addAddition(new Addition(addedLines, Integer.parseInt(matcher.group(1)), additionLength));
				}
			}
		}

		return patch;
	}

	public static class Builder {
		private List<String> baseLines;
		private Patch patch;
		private PrintWriter output;

		public Builder setBaseLines(List<String> baseLines) {
			this.baseLines = baseLines;
			return this;
		}

		public Builder setBaseReader(Reader reader) {
			return setBaseLines(Utils.getLinesFromReader(reader));
		}

		public Builder setBaseInputStream(InputStream inputStream) {
			return setBaseReader(new InputStreamReader(inputStream));
		}

		public Builder setBaseFile(File file) {
			return setBaseReader(Utils.getFileReader(file));
		}

		public Builder setPatch(Patch patch) {
			this.patch = patch;
			return this;
		}

		public Builder setPatchReader(Reader reader) throws InvalidPatchFormatException {
			return setPatch(readPatch(reader));
		}

		public Builder setPatchInputStream(InputStream inputStream) throws InvalidPatchFormatException {
			return setPatchReader(new InputStreamReader(inputStream));
		}

		public Builder setPatchFile(File file) throws InvalidPatchFormatException {
			return setPatchReader(Utils.getFileReader(file));
		}

		public Builder setOutput(PrintWriter output) {
			this.output = output;
			return this;
		}

		public Builder setOutputWriter(Writer writer) {
			return setOutput(new PrintWriter(writer));
		}

		public Builder setOutputStream(OutputStream outputStream) {
			return setOutputWriter(new OutputStreamWriter(outputStream));
		}

		public Builder setOutputFile(File outputFile) {
			return setOutputWriter(Utils.getFileWriter(outputFile));
		}

		public Patcher build() {
			if (baseLines == null) {
				throw new IllegalStateException("baseLines == null");
			}
			if (patch == null) {
				throw new IllegalStateException("patch == null");
			}
			if (output == null) {
				throw new IllegalStateException("output == null");
			}
			return new Patcher(baseLines, patch, output);
		}
	}

}
