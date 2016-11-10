package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

/**
 * A diff format, responsible for defining a <tt>line</tt>
 */
public interface IDiffFormat<T> {

	/**
	 * Returns the name of this diff format, e.g. "normal", "java", "byte"
	 */
	String getName();

	/**
	 * Reads all the lines from the given input stream and returns them
	 */
	default List<T> readElementsFromBaseFile(InputStream in) throws IOException {
		return readElementsFromPatchFile(new Scanner(in), Integer.MAX_VALUE);
	}

	/**
	 * Reads the given amount of lines from the given scanner, used in the
	 * text-based patch file format
	 */
	List<T> readElementsFromPatchFile(Scanner in, int count);

	/**
	 * Writes all the given elements to the output stream
	 */
	default void writeElementsToWorkFile(List<T> elements, OutputStream out) throws IOException {
		PrintWriter pw = new PrintWriter(out);
		printElementsToPatchFile(elements, pw);
		pw.flush();
		pw.close();
	}

	/**
	 * Prints all the given elements to the given PrintWriter. Used in the
	 * text-based patch file format
	 */
	void printElementsToPatchFile(List<T> elements, PrintWriter out);

	/**
	 * Serialize a line. Used in the binary patch file format
	 */
	void serializeElement(T element, DataOutputStream data) throws IOException;

	/**
	 * Deserialize a line. Used in the binary patch file format
	 */
	T deserializeElement(DataInputStream data) throws IOException;

}
