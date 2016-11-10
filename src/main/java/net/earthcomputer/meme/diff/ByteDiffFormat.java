package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * The byte diff format, compares files byte-by-byte
 */
public class ByteDiffFormat implements IDiffFormat<Byte> {

	@Override
	public String getName() {
		return "byte";
	}

	@Override
	public List<Byte> readElementsFromBaseFile(InputStream in) throws IOException {
		List<Byte> bytes = new ArrayList<Byte>();
		int _byte;
		while ((_byte = in.read()) != -1) {
			bytes.add((byte) _byte);
		}
		return bytes;
	}

	@Override
	public List<Byte> readElementsFromPatchFile(Scanner in, int count) {
		List<Byte> bytes = new ArrayList<Byte>();
		String bytesString = in.next("[0-9A-Za-z]{" + (count * 2) + "}");
		for (int i = 0; i < bytesString.length() - 1; i += 2) {
			bytes.add((byte) Integer.parseInt(bytesString.substring(i, i + 2), 16));
		}
		return bytes;
	}

	@Override
	public void writeElementsToWorkFile(List<Byte> elements, OutputStream out) throws IOException {
		byte[] bytes = new byte[elements.size()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = elements.get(i);
		}
		out.write(bytes);
	}

	@Override
	public void printElementsToPatchFile(List<Byte> elements, PrintWriter out) {
		for (Byte element : elements) {
			out.printf("%02X", element);
		}
		// Always print a newline
		out.println();
	}

	@Override
	public void serializeElement(Byte element, DataOutputStream data) throws IOException {
		data.writeByte(element);
	}

	@Override
	public Byte deserializeElement(DataInputStream data) throws IOException {
		return data.readByte();
	}

}
