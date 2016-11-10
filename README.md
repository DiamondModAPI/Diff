# Diff
MEMEDiff is a `diff` tool with a number of additional features which are useful to the MEME project.

A `diff` tool takes two files as input, known in MEMEDiff as the *base file* and the *work file*. It is helpful to think of the base file as an original version of the file and the work file as a modified version of the file. The `diff` tool then generates a *patch file*, which contains the changes needed to turn a copy of the base file into a copy of the work file. The `diff` tool can then take the base file and patch file as inputs, and generate a file identical to the work file.

## Additional features
* MEMEDiff supports two patch file formats: one text-based and one binary format. The binary format is more suitable for comparing two binary files, whilst the text-based format is more suitable for comparing plaintext.
* MEMEDiff can be configured to compare two files in different ways (known as the *diff format* or *patch format*, not to be confused with patch **file** format). The thing by which MEMEDiff is comparing is called the *line* for simplicity (not to be confused with lines that MEMEDiff compares only when in line-by-line mode):
 * Line-by-line - as done by most `diff` tools.
 * Byte-by-byte - as done by most `diff` tools that compare binary files.
 * Token-by-token - instead of lines or bytes, compare by Java tokens. This is useful if you don't want changes in multiline comments to change the line number and mess up the patch.
* MEMEDiff patch file formats do not show deleted lines - they only show which lines have been deleted.

## Patch file formats
### Text-based
Every text-based patch file starts thus:
```
meme-diff version 0
format normal
```
This header contains the file format version number and the diff format. Valid version numbers are currently only `0`. Valid diff formats are `normal`, `java` and `byte`.

The file then proceeds to list all the additions and deletions made by the patch. Additions and deletions (collectively called *changes*) can appear in any order, except that changes with a lower starting index (see below) must appear first.
```
!add <start_index>, <length>
<lines>
```
* `start_index` is the index of the line in the base file after which the lines should be added (with 1 as the first line, thus a value of 0 inserts this addition at the start of the file)
* `length` is the number of lines to be added by this single addition
* `lines` are the actual lines added. Remember that the definition of a *line* is different depending on the diff format, so the lines may not be delimited by a \n newline, \r carriage return or a \r\n combination of the two. However, the first line will always be preceded by one of these newline characters, and and last line will always be followed by a newline character if it does not end with one.
```
!delete <start_index>, <length>
```
* `start_inedx` is the index of first line in the base file to be deleted (starts at 1).
* `length` is the number of lines in the base file to be deleted

### Binary
The data types used in this file format are analogous to the data types used in Java, specifically read by an instance of [java.io.DataInput](http://docs.oracle.com/javase/7/docs/api/java/io/DataInput.html).

C-style representation of the binary patch file format:
```
struct binary_file_format {
  int magic; // Always equal to 0x4D444946, or "MDIF" in ASCII
  unsigned byte version; // Currently always equal to 0
  
  utf8 diff_format; // "normal", "java" or "byte" (usually "byte" in this patch file format)
  
  int num_additions; // This is signed, though cannot be less than 0
  addition additions[num_additions];
  
  int num_deletions;
  deletion deletions[num_deletions];
}

struct addition {
  int start_index;
  int length;
  L addedLines[length]; // Think of L as a type parameter (Java) or template (C++), serialized and deserialized by the diff format
}

struct deletion {
  int start_index;
  int length;
}
```

## Diff formats
### Normal
The normal diff format uses the word "line" in its literal sense (i.e. delimited by newline characters), and is similar to what is used by most `diff` tools. Lines are serialized and deserialized as strings of `utf8` type in the binary patch file format.
### Java
The Java diff format uses Java tokens, comments, whitespace and newlines as its "lines". The lines are not delimited by anything. Here is a list of what counts as a line in this format:
* `// Single line comments`
* 
```
/*
 * Multiline comments are counted as a single "line", even though they may contain newline characters
 */
```
* Characters `'c'`, `'\''`
* Strings `"Hello world"`, `"Contains \" escaped quote"`
* Words with [regex](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html) `[\w_\$]+`, such as `HelloWorld` and `containsLetters_andNumbers123`
* Multi-character Java operators such as `&&`, `||`, `+=`, `<<`, `>>>=`, etc.
* Single-character Java operators such as `.`, `;`, `+`, `(`, `>`, etc.
* Whitespace (matching the pattern `\s+`), e.g. `   `
* Newline `\n`, `\r` or `\r\n`

Lines are serialized and deserialized as literal strings of `utf8` type in the binary patch file format.

### Byte
A "line" in the byte diff format is a single byte. The byte is serialized and deserialized literally in the binary patch file format. However, because this byte can assume the value of a control character, it is necessary to convert this byte to a two-digit hexadecimal number in the text-based patch file format. These two-digit hexadecimal numbers are not delimited by anything, so are just run together.

## Installation
At the moment there is no online compiled version of MEMEDiff, so you will have to clone this repository and compile it yourself for the time being. (To get the Eclipse workspace, run `gradlew eclipse`).

## Use via the command-line
Altough MEMEDiff is intended to be used as a library, you can also invoke it via the command line as follows:
* Generating a patch file from a base file and work file:
```
java -jar memediff.jar <base-file> <work-file> <output-patch-file> [patch-format] [patch-file-format]
```
* Generating a work file from a base file and patch file:
```
java -cp memediff.jar net.earthcomputer.meme.diff.Patcher <base-file> <patch-file> <output-work-file> [patch-file-format]
```
