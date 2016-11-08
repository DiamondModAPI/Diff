package net.earthcomputer.meme.diff;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

class Utils {

	private Utils() {
	}

	static InputStream getFileInputStream(File file) {
		try {
			return new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new NoSuchFileException(file.getAbsolutePath(), e);
		}
	}

	static OutputStream getFileOutputStream(File file) {
		try {
			return new BufferedOutputStream(new FileOutputStream(file));
		} catch (IOException e) {
			throw new NoSuchFileException(file.getAbsolutePath(), e);
		}
	}

	private static final Pattern NEWLINE_PATTERN = Pattern.compile("\n");

	static boolean hasNextLine(Scanner scanner) {
		if (scanner.hasNextLine()) {
			return true;
		} else {
			scanner.useDelimiter(NEWLINE_PATTERN);
			return scanner.hasNext();
		}
	}
	
	static String nextLine(Scanner scanner) {
		if (scanner.hasNextLine()) {
			return scanner.nextLine();
		} else {
			scanner.useDelimiter(NEWLINE_PATTERN);
			return scanner.next();
		}
	}
}
