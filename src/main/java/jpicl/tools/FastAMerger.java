/*
 * FastAMerger.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.tools;

import jpicl.util.FastaParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Concatenates a set of parallel FASTA alignments into a single
 * supermatrix alignment. All input files must contain the same set of
 * taxon names (order-insensitive); the output preserves the taxon
 * order from the first input. Each output sequence is the
 * concatenation of that taxon's sequences across the inputs in input
 * order.
 *
 * As a side effect, prints a one-line position range to stdout for
 * each input — the 1-based, inclusive column range that input
 * occupies in the concatenated alignment, e.g. {@code RPOA.fasta: 1-2000}.
 */
public final class FastAMerger {

	/** Output FASTA line-wrap width (characters per sequence line). */
	public static final int LINE_WIDTH = 80;

	private FastAMerger() {}

	/**
	 * Concatenates {@code inputFiles} into {@code outputFile}. Each
	 * input is read with {@link FastaParser#parseAligned(Path)} so
	 * within-file length mismatches surface as IOException. Across-
	 * file taxon-set mismatches surface as IOException too.
	 *
	 * Prints a per-input range line to stdout.
	 */
	public static void merge(List<Path> inputFiles, Path outputFile) throws IOException {
		if (inputFiles.isEmpty()) {
			throw new IllegalArgumentException("No input files");
		}

		// LinkedHashMap so the output preserves the first file's
		// taxon order — a small but meaningful courtesy when the
		// user has curated a stable lineage ordering upstream.
		var byTaxon = new LinkedHashMap<String, StringBuilder>();
		int currentStart = 1;

		for (int i = 0; i < inputFiles.size(); i++) {
			var file = inputFiles.get(i);
			var sequences = FastaParser.parseAligned(file);

			if (i == 0) {
				for (var s : sequences) {
					byTaxon.put(s.name(), new StringBuilder(s.sequence()));
				}
			} else {
				// The size check + the every-name-known check below
				// together imply set equality (FastaParser already
				// guarantees no duplicates within a single file).
				if (sequences.size() != byTaxon.size()) {
					throw new IOException("File " + file.getFileName()
										  + " has " + sequences.size() + " taxa but expected "
										  + byTaxon.size());
				}
				for (var s : sequences) {
					var sb = byTaxon.get(s.name());
					if (sb == null) {
						throw new IOException("File " + file.getFileName()
											  + " contains unknown taxon '" + s.name() + "'");
					}
					sb.append(s.sequence());
				}
			}

			int len = sequences.get(0).sequence().length();
			int end = currentStart + len - 1;
			System.out.println(file.getFileName() + ": " + currentStart + "-" + end);
			currentStart = end + 1;
		}

		try (var w = Files.newBufferedWriter(outputFile, StandardCharsets.US_ASCII)) {
			for (var e : byTaxon.entrySet()) {
				w.write(">");
				w.write(e.getKey());
				w.newLine();
				writeWrapped(w, e.getValue().toString());
			}
		}
	}

	private static void writeWrapped(BufferedWriter w, String sequence) throws IOException {
		for (int i = 0; i < sequence.length(); i += LINE_WIDTH) {
			int end = Math.min(i + LINE_WIDTH, sequence.length());
			w.write(sequence, i, end - i);
			w.newLine();
		}
	}

	/**
	 * Command-line entry point. Inputs come in via argv; the output
	 * lands at {@code concatenated.fasta} as a sibling of the first
	 * input file.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("Usage: FastAMerger <input1.fasta> <input2.fasta> ...");
			System.err.println("Output: concatenated.fasta beside the first input file");
			System.exit(1);
		}
		var inputs = new ArrayList<Path>(args.length);
		for (var arg : args) inputs.add(Path.of(arg));
		var output = inputs.get(0).resolveSibling("concatenated.fasta");
		merge(inputs, output);
		System.out.println("→ " + output);
	}
}
