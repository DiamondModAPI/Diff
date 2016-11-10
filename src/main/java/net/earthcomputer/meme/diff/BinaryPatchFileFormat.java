package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.earthcomputer.meme.diff.Patch.Addition;
import net.earthcomputer.meme.diff.Patch.Deletion;

/**
 * The binary patch file format
 */
public class BinaryPatchFileFormat implements IPatchFileFormat {

	// @formatter:off
	private static final int MAGIC =
			('M' << 24) |
			('D' << 16) |
			('I' << 8) |
			('F' << 0);
	// @formatter:on
	private static final int VERSION = 0;

	@Override
	public String getName() {
		return "binary";
	}

	@Override
	public <T> PatchInfo<T> readPatch(InputStream in) throws InvalidPatchFormatException, IOException {
		DataInputStream data = new DataInputStream(in);
		if (data.readInt() != MAGIC) {
			throw new InvalidPatchFormatException("Invalid magic number");
		}
		int version = data.readUnsignedByte();
		switch (version) {
		case 0:
			return readPatchVersion0(data);
		default:
			throw new InvalidPatchFormatException("Unsupported patch format version: " + version);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> PatchInfo<T> readPatchVersion0(DataInputStream data)
			throws IOException, InvalidPatchFormatException {
		PatchInfo<T> patch = new PatchInfo<T>();

		patch.setFormat((IDiffFormat<T>) DiffFormats.getByName(data.readUTF()));
		if (patch.getFormat() == null) {
			throw new InvalidPatchFormatException("Invalid diff format");
		}
		patch.setPatch(new Patch<T>());

		int numAdditions = data.readInt();
		if (numAdditions < 0) {
			throw new InvalidPatchFormatException("numAdditions < 0");
		}
		for (int i = 0; i < numAdditions; i++) {
			int start = data.readInt();
			if (start < 0) {
				throw new InvalidPatchFormatException("start < 0");
			}
			int len = data.readInt();
			if (len < 0) {
				throw new InvalidPatchFormatException("len < 0");
			}
			List<T> addedLines = new ArrayList<T>(len);
			for (int j = 0; j < len; j++) {
				addedLines.add(patch.getFormat().deserializeElement(data));
			}
			patch.getPatch().addAddition(new Addition<T>(addedLines, start, len));
		}

		int numDeletions = data.readInt();
		if (numDeletions < 0) {
			throw new InvalidPatchFormatException("numDeletions < 0");
		}
		for (int i = 0; i < numDeletions; i++) {
			int start = data.readInt();
			if (start < 0) {
				throw new InvalidPatchFormatException("start < 0");
			}
			int len = data.readInt();
			if (len < 0) {
				throw new InvalidPatchFormatException("len < 0");
			}
			patch.getPatch().addDeletion(new Deletion<T>(start, len));
		}

		return patch;
	}

	@Override
	public <T> void writePatch(PatchInfo<T> patch, OutputStream out) throws IOException {
		DataOutputStream data = new DataOutputStream(out);

		data.writeInt(MAGIC);
		data.write(VERSION);

		data.writeUTF(patch.getFormat().getName());

		List<Addition<T>> additions = patch.getPatch().getAdditions();
		data.writeInt(additions.size());
		for (Addition<T> addition : additions) {
			data.writeInt(addition.getStart());
			data.writeInt(addition.getLength());
			List<T> addedLines = addition.getAddedLines();
			for (T addedLine : addedLines) {
				patch.getFormat().serializeElement(addedLine, data);
			}
		}

		List<Deletion<T>> deletions = patch.getPatch().getDeletions();
		data.writeInt(deletions.size());
		for (Deletion<T> deletion : deletions) {
			data.writeInt(deletion.getStart());
			data.writeInt(deletion.getLength());
		}
		
		data.flush();
		data.close();
	}

}
