/*
 * PhylipWriter.java Copyright (C) 2026 Daniel H. Huson
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes relaxed sequential Phylip format. PICL now accepts long taxon
 * names (up to ~16000 characters), so the writer no longer enforces
 * the old-style 10-character name limit. The output format is:
 *
 * <pre>
 * &lt;ntax&gt; &lt;nchars&gt;
 * Name<TAB>Sequence
 * Name<TAB>Sequence
 * ...
 * </pre>
 * <p>
 * Name and sequence are separated by a single tab. No interleaving;
 * the whole sequence sits on one line per taxon. All sequences must
 * have the same length (the matrix must be aligned).
 */
public final class PhylipWriter {

	private PhylipWriter() {
	}

	public static void write(Path file, List<Alignment.Sequence> sequences) throws IOException {
		if (sequences.isEmpty()) {
			throw new IllegalArgumentException("Cannot write empty Phylip file");
		}
		var nchars = sequences.get(0).sequence().length();
		for (var s : sequences) {
			if (s.sequence().length() != nchars) {
				throw new IllegalArgumentException("Sequence '" + s.name()
												   + "' has length " + s.sequence().length()
												   + " but expected " + nchars + " (matrix not aligned)");
			}
			if (s.name() == null || s.name().isBlank()) {
				throw new IllegalArgumentException("Empty taxon name in sequence list");
			}
		}

		var sb = new StringBuilder();
		sb.append(' ').append(sequences.size()).append(' ').append(nchars).append('\n');
		for (var s : sequences) {
			sb.append(s.name()).append('\t').append(s.sequence()).append('\n');
		}
		Files.writeString(file, sb.toString(), StandardCharsets.US_ASCII);
	}
}
