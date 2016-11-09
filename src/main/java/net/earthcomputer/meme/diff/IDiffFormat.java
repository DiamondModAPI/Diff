package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public interface IDiffFormat<T> {
	
	String getName();
	
	List<T> readElements(Scanner in, int count);
	
	void printElements(List<T> elements, PrintWriter out);
	
	void serializeElement(T element, DataOutputStream data) throws IOException;
	
	T deserializeElement(DataInputStream data) throws IOException;
	
}
