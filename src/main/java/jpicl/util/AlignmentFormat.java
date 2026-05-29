/*
 * AlignmentFormat.java Copyright (C) 2026 Daniel H. Huson
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

/**
 * Identifies sequence-alignment file formats by inspecting the first
 * non-blank line. PICL only consumes old-style Phylip; everything else
 * is converted (with a name-remapping step) before the C side sees it.
 * <p>
 * Detection rules:
 * <ul>
 *   <li>FASTA: first non-blank line starts with {@code '>'}.</li>
 *   <li>PHYLIP: first non-blank line is two whitespace-separated
 *       integers (taxon count and character count).</li>
 *   <li>UNKNOWN: anything else, including empty files.</li>
 * </ul>
 */
public enum AlignmentFormat {
	PHYLIP, FASTA, UNKNOWN;

	/**
	 * Detects the format of {@code file} by reading just enough of the
	 * head to make a decision (no full parse). Returns UNKNOWN rather
	 * than throwing if the file is empty or unrecognisable.
	 */
	public static AlignmentFormat detect(Path file) throws IOException {
		try (var lines = Files.lines(file)) {
			var first = lines.map(String::trim)
					.filter(s -> !s.isEmpty())
					.findFirst();
			if (first.isEmpty()) return UNKNOWN;
			var line = first.get();
			if (line.startsWith(">")) return FASTA;
			if (looksLikePhylipHeader(line)) return PHYLIP;
			return UNKNOWN;
		}
	}

	/**
	 * Two whitespace-separated positive integers ⇒ Phylip header.
	 */
	private static boolean looksLikePhylipHeader(String line) {
		var parts = line.trim().split("\\s+");
		if (parts.length < 2) return false;
		try {
			var ntax = Integer.parseInt(parts[0]);
			var nchars = Integer.parseInt(parts[1]);
			return ntax > 0 && nchars > 0;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
