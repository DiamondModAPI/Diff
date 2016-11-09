package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NormalDiffFormat implements IDiffFormat<String> {

	@Override
	public String getName() {
		return "normal";
	}

	@Override
	public List<String> readElementsFromPatchFile(Scanner in, int count) {
		List<String> lines = new ArrayList<String>();
		for (int i = 0; i < count && Utils.hasNextLine(in); i++) {
			lines.add(Utils.nextLine(in));
		}
		return lines;
	}

	@Override
	public void printElementsToPatchFile(List<String> elements, PrintWriter out) {
		for (String element : elements) {
			out.println(element);
		}
	}

	@Override
	public void serializeElement(String element, DataOutputStream data) throws IOException {
		data.writeUTF(element);
	}

	@Override
	public String deserializeElement(DataInputStream data) throws IOException {
		return data.readUTF();
	}

}
