package net.earthcomputer.meme.diff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaDiffFormat implements IDiffFormat<JavaDiffFormat.Token> {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern SINGLE_CHAR = Pattern.compile("[^\\w\\s]");
	private static final Pattern MULTICHAR_OPERATOR = Pattern
			.compile("==|&&|\\|\\||>>>?|<<|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|>>>?=|<<=|->|::|!=|<=|>=|\\+\\+|--");
	private static final Pattern WORD = Pattern.compile("[\\w_\\$]+");
	private static final Pattern SINGLE_QUOTED_END = Pattern.compile(".*?'");
	private static final Pattern SINGLE_QUOTED_TEXT = Pattern.compile("'.+?'");
	private static final Pattern DOUBLE_QUOTED_END = Pattern.compile(".*?\"");
	private static final Pattern DOUBLE_QUOTED_TEXT = Pattern.compile("\".*?\"");
	private static final Pattern MULTILINE_COMMENT_START = Pattern.compile("/\\*");
	private static final Pattern MULTILINE_COMMENT_END = Pattern.compile("\\*/");
	private static final Pattern SINGLELINE_COMMENT = Pattern.compile("//.*?$");
	private static final Pattern TOKEN_PATTERN;
	static {
		Pattern[] patterns = { SINGLELINE_COMMENT, MULTILINE_COMMENT_START, DOUBLE_QUOTED_TEXT, SINGLE_QUOTED_TEXT,
				WORD, MULTICHAR_OPERATOR, SINGLE_CHAR, WHITESPACE };
		String pattern = "";
		for (Pattern p : patterns) {
			if (!pattern.isEmpty()) {
				pattern += "|";
			}
			pattern += "(" + p.pattern() + ")";
		}
		TOKEN_PATTERN = Pattern.compile(pattern);
	}

	@Override
	public String getName() {
		return "java";
	}

	@Override
	public List<Token> readElementsFromPatchFile(Scanner in, int count) {
		// Most tokens appear on the same line, so the basic concept is we
		// iterate through the lines and then search for tokens in each line.
		// The only multiline token, the multiline comment, is treated as a
		// special case throughout this code.

		List<Token> tokens = new ArrayList<Token>();
		String multilineComment = null;

		// Iterate through the lines
		lineLoop: while ((tokens.size() < count || multilineComment != null) && Utils.hasNextLine(in)) {
			String line = Utils.nextLine(in);

			Matcher tokenMatcher = TOKEN_PATTERN.matcher(line);

			// Check if we are still in a multiline comment
			if (multilineComment != null) {
				// Try and find an end to the comment on this line
				tokenMatcher.usePattern(MULTILINE_COMMENT_END);
				if (tokenMatcher.find()) {
					// There is an end, continue searching this line
					multilineComment += line.substring(0, tokenMatcher.start());
					multilineComment += tokenMatcher.group();
					tokenMatcher.usePattern(TOKEN_PATTERN);
					tokens.add(new Token(multilineComment));
					multilineComment = null;
					// Check if we shouldn't be consuming any more tokens
					if (tokens.size() >= count) {
						break lineLoop;
					}
				} else {
					// Just add this whole line to the existing multiline
					// comment and continue to the next line
					multilineComment += line + "\n";
					continue lineLoop;
				}
			}

			// Iterate through the tokens in this line
			tokenLoop: while (tokens.size() < count && tokenMatcher.find()) {
				String token = tokenMatcher.group();

				if (SINGLE_QUOTED_TEXT.matcher(token).matches()) {
					// Search for \ escapes in single-quotes
					tokenMatcher.usePattern(SINGLE_QUOTED_END);
					while (isEscaped(token, token.length() - 1)) {
						if (!tokenMatcher.find()) {
							break tokenLoop;
						}
						token += tokenMatcher.group();
					}
					tokenMatcher.usePattern(TOKEN_PATTERN);
				} else if (DOUBLE_QUOTED_TEXT.matcher(token).matches()) {
					// Search for \ escapes in double-quotes
					tokenMatcher.usePattern(DOUBLE_QUOTED_END);
					while (isEscaped(token, token.length() - 1)) {
						if (!tokenMatcher.find()) {
							break tokenLoop;
						}
						token += tokenMatcher.group();
					}
					tokenMatcher.usePattern(TOKEN_PATTERN);
				} else if (MULTILINE_COMMENT_START.matcher(token).matches()) {
					multilineComment = token;
					int commentStart = tokenMatcher.end();

					// Search for a possible end on the same line
					tokenMatcher.usePattern(MULTILINE_COMMENT_END);
					if (tokenMatcher.find()) {
						// Just complete this token with the rest of the
						// comment, and continue as if nothing happened
						multilineComment += line.substring(commentStart, tokenMatcher.start());
						multilineComment += tokenMatcher.group();
						token = multilineComment;
						multilineComment = null;
						tokenMatcher.usePattern(TOKEN_PATTERN);
					} else {
						// Add the rest of this line to the multiline comment
						// and continue to the next line
						multilineComment += line.substring(commentStart);
						multilineComment += "\n";
						continue lineLoop;
					}
				}

				// Finally, add this token to the token list
				tokens.add(new Token(token));
			}

			// Newline counts as a token
			// Make sure we don't consume too many tokens
			if (tokens.size() < count) {
				tokens.add(new Token("\n"));
			}
		}
		return tokens;
	}

	private static boolean isEscaped(String str, int index) {
		return index > 0 && str.charAt(index - 1) == '\\' && !isEscaped(str, index - 1);
	}

	@Override
	public void printElementsToPatchFile(List<Token> elements, PrintWriter out) {
		// We need to be careful we use the right newline character on the right
		// operating system
		for (Token token : elements) {
			if (token.isNewLine()) {
				out.println();
			} else {
				String[] parts = token.getValue().split("\n");
				for (int i = 0; i < parts.length; i++) {
					if (i != 0) {
						out.println();
					}
					out.print(parts[i]);
				}
			}
		}
		if (!elements.get(elements.size() - 1).getValue().endsWith("\n")) {
			out.println();
		}
	}

	@Override
	public void serializeElement(Token element, DataOutputStream data) throws IOException {
		data.writeUTF(element.getValue());
	}

	@Override
	public Token deserializeElement(DataInputStream data) throws IOException {
		return new Token(data.readUTF());
	}

	public static class Token {
		private String value;

		public Token(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public boolean isNewLine() {
			return value.equals("\n");
		}

		public boolean isWhitespace() {
			return value.matches("\\s+");
		}

		@Override
		public String toString() {
			if (isNewLine()) {
				return "<newline>";
			} else if (isWhitespace()) {
				return "<whitespace> \"" + value + "\"";
			} else {
				return "<token> " + value;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Token))
				return false;
			Token other = (Token) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

}
