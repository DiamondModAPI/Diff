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
}
