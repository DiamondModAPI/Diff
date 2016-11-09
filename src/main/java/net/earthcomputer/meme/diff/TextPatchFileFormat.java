package net.earthcomputer.meme.diff;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

public class TextPatchFileFormat implements IPatchFileFormat {

	public static final int VERSION = 0;

	private static final Pattern HEADER_PATTERN = Pattern.compile("\\s*meme-diff\\s+version\\s+(\\d+)\\s*");
	private static final Pattern DIFF_FORMAT_PATTERN = Pattern.compile("\\s*format\\s+(\\w+)\\s*");
	private static final Pattern DELETION_PATTERN = Pattern.compile("\\s*!delete\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");
	private static final Pattern ADDITION_PATTERN = Pattern.compile("\\s*!add\\s+(\\d+)\\s*,\\s*(\\d+)\\s*");

	@Override
	public String getName() {
		return "text";
	}

	@Override
	public <T> PatchInfo<T> readPatch(InputStream in) throws InvalidPatchFormatException {
		PatchInfo<T> patchInfo = new PatchInfo<T>();
		patchInfo.setPatch(new Patch<T>());
		Scanner scanner = new Scanner(in);

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
		patchInfo.setFormat((IDiffFormat<T>) DiffFormats.getByName(formatMatcher.group(1)));
		if (patchInfo.getFormat() == null) {
			throw new InvalidPatchFormatException("Unrecognized format: \"" + formatMatcher.group(1) + "\"");
		}
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			Matcher matcher = DELETION_PATTERN.matcher(line);
			if (matcher.matches()) {
				patchInfo.getPatch().addDeletion(
						new Deletion<T>(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
			} else {
				matcher = ADDITION_PATTERN.matcher(line);
				if (matcher.matches()) {
					int additionLength = Integer.parseInt(matcher.group(2));

					List<T> addedLines = patchInfo.getFormat().readElements(scanner, additionLength);

					patchInfo.getPatch().addAddition(
							new Addition<T>(addedLines, Integer.parseInt(matcher.group(1)), additionLength));
				}
			}
		}

		return patchInfo;
	}

	@Override
	public <T> void writePatch(PatchInfo<T> patch, OutputStream out) {
		PrintWriter pw = new PrintWriter(out);

		Iterator<Addition<T>> additionItr = patch.getPatch().getAdditions().iterator();
		Iterator<Deletion<T>> deletionItr = patch.getPatch().getDeletions().iterator();
		Addition<T> addition = additionItr.hasNext() ? additionItr.next() : null;
		Deletion<T> deletion = deletionItr.hasNext() ? deletionItr.next() : null;

		pw.println("meme-diff version " + VERSION);
		pw.println("format " + patch.getFormat().getName());

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
					pw.println();
					prevStart = deletion.getStart();
				}
				pw.println("!delete " + deletion.getStart() + "," + deletion.getLength());
				deletion = deletionItr.hasNext() ? deletionItr.next() : null;
			} else {
				if (addition.getStart() != prevStart) {
					pw.println();
					prevStart = addition.getStart();
				}
				pw.println("!add " + addition.getStart() + "," + addition.getLength());
				patch.getFormat().printElements(addition.getAddedLines(), pw);
				addition = additionItr.hasNext() ? additionItr.next() : null;
			}
		}

		// Make sure the file ends with a newline
		pw.println();

		pw.flush();
	}

}
