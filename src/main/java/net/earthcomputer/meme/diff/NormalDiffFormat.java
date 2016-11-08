package net.earthcomputer.meme.diff;

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
	public List<String> readElements(Scanner in, int count) {
		if (count == -1) {
			count = Integer.MAX_VALUE;
		}
		List<String> lines = new ArrayList<String>();
		for (int i = 0; i < count && Utils.hasNextLine(in); i++) {
			lines.add(Utils.nextLine(in));
		}
		return lines;
	}

	@Override
	public void printElements(List<String> elements, PrintWriter out) {
		for (String element : elements) {
			out.println(element);
		}
	}

}
