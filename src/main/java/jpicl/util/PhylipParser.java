/*
 * PhylipParser.java Copyright (C) 2026 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jpicl.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Parses sequential Phylip format — the symmetric inverse of
 * {@link PhylipWriter}. A file in this format starts with a header
 * line "{@code <ntax> <nchars>}" followed by exactly {@code ntax}
 * data lines, each of the form "name &lt;whitespace&gt; sequence".
 *
 * <p>Returns a list of {@link Alignment.Sequence} records — the same
 * type {@link FastaParser#parseAligned(Path)} returns — so callers
 * can treat parsed Phylip and parsed FASTA interchangeably.
 *
 * <p>Accepts both the old-style 10-char-padded layout (where the
 * trailing pad spaces double as the separator) and modern relaxed
 * Phylip (name and sequence separated by tab or arbitrary whitespace).
 * The split happens at the first whitespace on each data line.
 *
 * <p>Strict checks: rejects empty files, malformed headers, lines
 * with no whitespace separator, empty names, duplicate names, and
 * sequences whose length doesn't match the declared {@code nchars}.
 * Permissive on blank lines and on whitespace embedded in the
 * sequence portion (stripped before length validation).
 *
 * <p>Limitations: only sequential Phylip is supported. Interleaved
 * Phylip (sequences split across multiple blocks) is not handled —
 * callers should pre-convert.
 */
public final class PhylipParser {

	private PhylipParser() {
	}

	public static List<Alignment.Sequence> parse(Path file) throws IOException {
		var lines = Files.readAllLines(file);
		if (lines.isEmpty()) {
			throw new IOException("Phylip: empty file");
		}

		// Header: <ntax> <nchars>, possibly with leading whitespace.
		var header = lines.get(0).trim().split("\\s+");
		if (header.length < 2) {
			throw new IOException("Phylip: header line must contain ntax and nchars (got '"
								  + lines.get(0) + "')");
		}
		int ntax;
		int nchars;
		try {
			ntax = Integer.parseInt(header[0]);
			nchars = Integer.parseInt(header[1]);
		} catch (NumberFormatException ex) {
			throw new IOException("Phylip: header line malformed: '" + lines.get(0) + "'");
		}
		if (ntax <= 0 || nchars <= 0) {
			throw new IOException("Phylip: ntax and nchars must be positive (got "
								  + ntax + " and " + nchars + ")");
		}

		var records = new ArrayList<Alignment.Sequence>(ntax);
		var seenNames = new HashSet<String>();

		for (int i = 1; i < lines.size() && records.size() < ntax; i++) {
			var line = lines.get(i);
			if (line.isEmpty()) continue;

			// Split at the first whitespace — works for both old-style
			// (10-char-padded names → split at the trailing pad space)
			// and relaxed (name<TAB>sequence or name<space>sequence).
			int sep = firstWhitespaceIndex(line);
			if (sep < 0) {
				throw new IOException("Phylip: line " + (i + 1)
									  + " has no whitespace separator between name and sequence: '"
									  + line + "'");
			}
			var name = line.substring(0, sep).trim();
			var sequence = stripWhitespace(line.substring(sep));

			if (name.isEmpty()) {
				throw new IOException("Phylip: empty name on line " + (i + 1));
			}
			if (sequence.length() != nchars) {
				throw new IOException("Phylip: sequence '" + name + "' has length "
									  + sequence.length() + " but header declared " + nchars);
			}
			if (!seenNames.add(name)) {
				throw new IOException("Phylip: duplicate name '" + name + "'");
			}
			records.add(new Alignment.Sequence(name, sequence));
		}

		if (records.size() != ntax) {
			throw new IOException("Phylip: header declared " + ntax
								  + " sequences but only " + records.size() + " were found");
		}
		return records;
	}

	private static int firstWhitespaceIndex(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) return i;
		}
		return -1;
	}

	private static String stripWhitespace(String s) {
		var sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			var ch = s.charAt(i);
			if (!Character.isWhitespace(ch)) sb.append(ch);
		}
		return sb.toString();
	}
}
