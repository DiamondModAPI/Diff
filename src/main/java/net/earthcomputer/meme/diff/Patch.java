package net.earthcomputer.meme.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Patch {

	private List<Addition> additions = new ArrayList<Addition>();
	private List<Deletion> deletions = new ArrayList<Deletion>();

	public void addAddition(Addition addition) {
		additions.add(addition);
	}

	public void addDeletion(Deletion deletion) {
		deletions.add(deletion);
	}

	public List<Addition> getAdditions() {
		return Collections.unmodifiableList(additions);
	}

	public List<Deletion> getDeletions() {
		return Collections.unmodifiableList(deletions);
	}

	public static abstract class Change {
		private final int start;
		private final int length;

		public Change(int start, int length) {
			this.start = start;
			this.length = length;
		}

		public int getStart() {
			return start;
		}

		public int getLength() {
			return length;
		}
	}

	public static class Addition extends Change {
		private List<String> addedLines;

		public Addition(List<String> addedLines, int start, int length) {
			super(start, length);
			this.addedLines = addedLines;
		}

		public List<String> getAddedLines() {
			return addedLines;
		}
	}

	public static class Deletion extends Change {
		public Deletion(int start, int length) {
			super(start, length);
		}
	}

}
