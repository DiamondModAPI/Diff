package net.earthcomputer.meme.diff;

import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public interface IDiffFormat<T> {

	String getName();
	
	List<T> readElements(Scanner in, int count);
	
	void printElements(List<T> elements, PrintWriter out);
	
}
