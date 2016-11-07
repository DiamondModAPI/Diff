package net.earthcomputer.meme.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Patch<T> {

	private List<Addition<T>> additions = new ArrayList<Addition<T>>();
	private List<Deletion<T>> deletions = new ArrayList<Deletion<T>>();

	public void addAddition(Addition<T> addition) {
		additions.add(addition);
	}

	public void addDeletion(Deletion<T> deletion) {
		deletions.add(deletion);
	}

	public List<Addition<T>> getAdditions() {
		return Collections.unmodifiableList(additions);
	}

	public List<Deletion<T>> getDeletions() {
		return Collections.unmodifiableList(deletions);
	}

	public static abstract class Change<T> {
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

	public static class Addition<T> extends Change<T> {
		private List<T> addedLines;

		public Addition(List<T> currentAddition, int start, int length) {
			super(start, length);
			this.addedLines = currentAddition;
		}

		public List<T> getAddedLines() {
			return addedLines;
		}
	}

	public static class Deletion<T> extends Change<T> {
		public Deletion(int start, int length) {
			super(start, length);
		}
	}

}
