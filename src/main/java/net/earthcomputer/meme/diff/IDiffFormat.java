package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public interface IDiffFormat<T> {

	String getName();

	default List<T> readElementsFromBaseFile(InputStream in) throws IOException {
		return readElementsFromPatchFile(new Scanner(in), Integer.MAX_VALUE);
	}

	List<T> readElementsFromPatchFile(Scanner in, int count);

	default void writeElementsToWorkFile(List<T> elements, OutputStream out) throws IOException {
		PrintWriter pw = new PrintWriter(out);
		printElementsToPatchFile(elements, pw);
		pw.flush();
		pw.close();
	}

	void printElementsToPatchFile(List<T> elements, PrintWriter out);

	void serializeElement(T element, DataOutputStream data) throws IOException;

	T deserializeElement(DataInputStream data) throws IOException;

}
