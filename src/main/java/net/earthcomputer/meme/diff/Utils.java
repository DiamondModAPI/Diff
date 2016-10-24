package net.earthcomputer.meme.diff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Utils {

	private Utils() {
	}

	static List<String> getLinesFromReader(Reader reader) {
		List<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(reader);
		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}
		scanner.useDelimiter("\n");
		if (scanner.hasNext()) {
			lines.add(scanner.next());
		}
		scanner.close();
		return lines;
	}

	static Reader getFileReader(File file) {
		try {
			return new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new NoSuchFileException(file.getAbsolutePath(), e);
		}
	}

	static Writer getFileWriter(File file) {
		try {
			return new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new NoSuchFileException(file.getAbsolutePath(), e);
		}
	}
}
