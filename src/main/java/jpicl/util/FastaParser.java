/*
 * FastaParser.java Copyright (C) 2026 Daniel H. Huson
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
 * Parses FASTA-format sequence files into a list of {@link Alignment.Sequence}
 * records. Designed for the common conventions used by phylogenetics
 * databases — name = first whitespace-separated token after the
 * {@code '>'} marker; sequence lines (which may wrap arbitrarily)
 * concatenated until the next {@code '>'} or EOF.
 *
 * <p>Strict checks: rejects empty inputs, sequences with no body,
 * duplicate names, and (when {@link #parseAligned(Path)} is used)
 * unequal sequence lengths. Permissive on whitespace and blank lines.
 */
public final class FastaParser {

	private FastaParser() {
	}

	/**
	 * Parses {@code file} as FASTA and returns the records in source
	 * order. Sequences may legally have different lengths — caller
	 * must validate uniform length if the alignment is meant to be
	 * a Phylip matrix.
	 */
	public static List<Alignment.Sequence> parse(Path file) throws IOException {
		var records = new ArrayList<Alignment.Sequence>();
		var seenNames = new HashSet<String>();

		String currentName = null;
		var currentSeq = new StringBuilder();

		for (var rawLine : Files.readAllLines(file)) {
			var line = rawLine.trim();
			if (line.isEmpty() || line.startsWith(";")) continue; // blanks + comments
			if (line.startsWith(">")) {
				flush(records, seenNames, currentName, currentSeq);
				currentName = parseName(line);
				currentSeq.setLength(0);
			} else {
				if (currentName == null) {
					throw new IOException("FASTA: sequence content before first '>' header");
				}
				// Strip in-sequence whitespace; preserve case (PICL is case-sensitive
				// for some character sets — leave that policy to the caller).
				for (int i = 0; i < line.length(); i++) {
					var ch = line.charAt(i);
					if (!Character.isWhitespace(ch)) currentSeq.append(ch);
				}
			}
		}
		flush(records, seenNames, currentName, currentSeq);

		if (records.isEmpty()) {
			throw new IOException("FASTA: no records found");
		}
		return records;
	}

	/**
	 * Like {@link #parse(Path)} but additionally verifies that every
	 * sequence has the same length, the constraint Phylip imposes on
	 * an alignment matrix.
	 */
	public static List<Alignment.Sequence> parseAligned(Path file) throws IOException {
		var records = parse(file);
		var len = records.get(0).sequence().length();
		for (var r : records) {
			if (r.sequence().length() != len) {
				throw new IOException("FASTA: sequences have unequal lengths — '"
									  + records.get(0).name() + "' is " + len
									  + " chars, '" + r.name() + "' is " + r.sequence().length()
									  + " (expected an aligned matrix)");
			}
		}
		return records;
	}

	private static String parseName(String headerLine) {
		// Drop the leading '>', then take everything up to the first whitespace.
		// Anything after the first whitespace is treated as a description and
		// discarded — same convention as most phylogenetics tools.
		var afterMarker = headerLine.substring(1).trim();
		if (afterMarker.isEmpty()) {
			throw new IllegalArgumentException("FASTA: empty header (just '>')");
		}
		var firstWs = -1;
		for (int i = 0; i < afterMarker.length(); i++) {
			if (Character.isWhitespace(afterMarker.charAt(i))) {
				firstWs = i;
				break;
			}
		}
		return firstWs < 0 ? afterMarker : afterMarker.substring(0, firstWs);
	}

	private static void flush(List<Alignment.Sequence> records, HashSet<String> seenNames,
							  String name, StringBuilder seq) throws IOException {
		if (name == null) return;
		if (seq.isEmpty()) {
			throw new IOException("FASTA: record '" + name + "' has no sequence");
		}
		if (!seenNames.add(name)) {
			throw new IOException("FASTA: duplicate name '" + name + "'");
		}
		records.add(new Alignment.Sequence(name, seq.toString()));
	}
}
