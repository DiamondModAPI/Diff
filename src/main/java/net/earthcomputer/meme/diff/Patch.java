package net.earthcomputer.meme.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains additions and deletions to be made to the base file to produce the
 * work file
 */
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

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Additions:");
		for (Addition<T> addition : additions) {
			str.append("\n" + addition);
		}
		str.append("\nDeletions:");
		for (Deletion<T> deletion : deletions) {
			str.append("\n" + deletion);
		}
		return str.toString();
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

		@Override
		public String toString() {
			return "from " + start + ", len " + length;
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

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder(super.toString());
			str.append(':');
			for (T addedLine : addedLines) {
				str.append("\n  ").append(addedLine);
			}
			return str.toString();
		}
	}

	public static class Deletion<T> extends Change<T> {
		public Deletion(int start, int length) {
			super(start, length);
		}
	}

}
