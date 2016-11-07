package net.earthcomputer.meme.diff;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

public class Patcher<T> {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println(
					"java -cp memediff.jar net.earthcomputer.diamond.diff.Patcher <base-file> <patch-file> <output-work-file>");
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
			new Patcher.Builder<Object>().setPatchFile(patchFile).setBaseFile(baseFile).setOutputFile(workFile).build()
					.writeWorkFile();
		} catch (InvalidPatchFormatException e) {
			System.err.println("The patch file had invalid format: " + e.getMessage());
			System.exit(1);
			return;
		}
	}

	private final IDiffFormat<T> format;
	private final List<T> baseLines;
	private final Patch<T> patch;
	private final PrintWriter output;

	private static final Pattern HEADER_PATTERN = Pattern.compile("\\s*meme-diff\\s+version\\s+(\\d+)\\s*");
	private static final Pattern DIFF_FORMAT_PATTERN = Pattern.compile("\\s*format\\s+(\\w+)\\s*");
	private static final Pattern DELETION_PATTERN = Pattern.compile("\\s*!delete\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");
	private static final Pattern ADDITION_PATTERN = Pattern.compile("\\s*!add\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");

	private Patcher(IDiffFormat<T> format, List<T> baseLines, Patch<T> patch, PrintWriter output) {
		this.format = format;
		this.baseLines = baseLines;
		this.patch = patch;
		this.output = output;
	}

	public void writeWorkFile() {
		List<T> workLines = computeWorkFile();

		format.printElements(workLines, output);

		output.flush();
		output.close();
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

	public static <T> PatchInfo<T> readPatch(Reader reader) throws InvalidPatchFormatException {
		PatchInfo<T> patchInfo = new PatchInfo<T>();
		patchInfo.patch = new Patch<T>();
		Scanner scanner = new Scanner(reader);

		try {
			String header = scanner.nextLine();
			Matcher headerMatcher = HEADER_PATTERN.matcher(header);
			if (!headerMatcher.matches()) {
				throw new InvalidPatchFormatException("Invalid header \"" + header + "\"");
			}
			int patchVersion = Integer.parseInt(headerMatcher.group(1));

			switch (patchVersion) {
			case 0:
				return readPatchVersion0(patchInfo, scanner);
			default:
				throw new InvalidPatchFormatException("Unsupported patch file version: " + patchVersion);
			}
		} catch (NoSuchElementException e) {
			throw new InvalidPatchFormatException("Reached the end of the file unexpectedly");
		} catch (Exception e) {
			throw new InvalidPatchFormatException("Miscellaneous error");
		} finally {
			scanner.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> PatchInfo<T> readPatchVersion0(PatchInfo<T> patchInfo, Scanner scanner)
			throws InvalidPatchFormatException {
		String formatLine = scanner.nextLine();
		Matcher formatMatcher = DIFF_FORMAT_PATTERN.matcher(formatLine);
		if (!formatMatcher.matches()) {
			throw new InvalidPatchFormatException("Invalid format line: \"" + formatLine + "\"");
		}
		patchInfo.format = (IDiffFormat<T>) DiffFormats.getByName(formatMatcher.group(1));
		if (patchInfo.format == null) {
			throw new InvalidPatchFormatException("Unrecognized format: \"" + formatMatcher.group(1) + "\"");
		}
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			Matcher matcher = DELETION_PATTERN.matcher(line);
			if (matcher.matches()) {
				patchInfo.patch.addDeletion(
						new Deletion<T>(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
			} else {
				matcher = ADDITION_PATTERN.matcher(line);
				if (matcher.matches()) {
					int additionLength = Integer.parseInt(matcher.group(2));

					List<T> addedLines = patchInfo.format.readElements(scanner, additionLength);

					patchInfo.patch.addAddition(
							new Addition<T>(addedLines, Integer.parseInt(matcher.group(1)), additionLength));
				}
			}
		}

		return patchInfo;
	}

	private static class PatchInfo<T> {
		private Patch<T> patch;
		private IDiffFormat<T> format;
	}

	public static class Builder<T> {
		private IDiffFormat<T> format;
		private List<T> baseLines;
		private Patch<T> patch;
		private PrintWriter output;

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
			return setBaseLines(format.readElements(new Scanner(inputStream), -1));
		}

		public Builder<T> setBaseFile(File file) {
			return setBaseInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setPatch(Patch<T> patch) {
			this.patch = patch;
			return this;
		}

		public Builder<T> setPatchReader(Reader reader) throws InvalidPatchFormatException {
			PatchInfo<T> patchInfo = readPatch(reader);
			return setPatch(patchInfo.patch).setDiffFormat(patchInfo.format);
		}

		public Builder<T> setPatchInputStream(InputStream inputStream) throws InvalidPatchFormatException {
			return setPatchReader(new InputStreamReader(inputStream));
		}

		public Builder<T> setPatchFile(File file) throws InvalidPatchFormatException {
			return setPatchInputStream(Utils.getFileInputStream(file));
		}

		public Builder<T> setOutput(PrintWriter output) {
			this.output = output;
			return this;
		}

		public Builder<T> setOutputStream(OutputStream outputStream) {
			return setOutput(new PrintWriter(outputStream));
		}

		public Builder<T> setOutputFile(File outputFile) {
			return setOutputStream(Utils.getFileOutputStream(outputFile));
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
