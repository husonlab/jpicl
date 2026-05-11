/*
 * Alignment.java Copyright (C) 2026 Daniel H. Huson
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
import java.nio.file.Path;
import java.util.List;

public class Alignment {
	public record Sequence(String name, String sequence) {
	}

	private Alignment() {
	}

	public static List<Sequence> parse(Path file) throws IOException {
		var format = AlignmentFormat.detect(file);
		if (format == AlignmentFormat.PHYLIP) {
			return PhylipParser.parse(file);
		} else if (format == AlignmentFormat.FASTA) {
			var sequences = FastaParser.parse(file);
			if (!equalLengths(sequences))
				throw new IOException("Sequences have different lengths");
			return sequences;
		} else throw new IOException("Unknown format");
	}

	public static boolean equalLengths(List<Sequence> sequences) {
		if (sequences.isEmpty())
			return true;
		else {
			var length = sequences.get(0).sequence().length();
			return sequences.stream().map(Sequence::sequence).allMatch(s -> s.length() == length);
		}
	}
}
