/*
 * NewickWriter.java Copyright (C) 2026 Daniel H. Huson
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

import java.util.Locale;

public class NewickWriter {

	/**
	 * Writes a tree in Newick format.
	 * <p>
	 * Examples:
	 * <p>
	 * (A,B);
	 * (A:0.1,B:0.2);
	 * (A:0.1:95,B:0.2:90);
	 *
	 * @param root            root node
	 * @param showWeights     include branch lengths
	 * @param showConfidences include confidence values
	 * @return newick string ending with ';'
	 */
	public static String write(TreeNode root,
							   boolean showWeights,
							   boolean showConfidences) {

		StringBuilder buf = new StringBuilder();

		writeRec(root, buf, showWeights, showConfidences);

		buf.append(";");

		return buf.toString();
	}

	private static void writeRec(TreeNode node,
								 StringBuilder buf,
								 boolean showWeights,
								 boolean showConfidences) {

		// children
		if (!node.getChildren().isEmpty()) {

			buf.append("(");

			for (int i = 0; i < node.getChildren().size(); i++) {

				if (i > 0)
					buf.append(",");

				writeRec(node.getChildren().get(i),
						buf,
						showWeights,
						showConfidences);
			}

			buf.append(")");
		}

		// label
		if (node.getLabel() != null
			&& !node.getLabel().isEmpty()) {

			buf.append(escapeLabel(node.getLabel()));
		}

		// branch data
		boolean wroteWeight = false;

		if (showWeights && node.getWeight() != null) {

			buf.append(":");
			buf.append(format(node.getWeight()));

			wroteWeight = true;
		}

		if (showConfidences && node.getConfidence() != null) {

			/*
			 * Standard requested format:
			 *
			 *   :weight:confidence
			 *
			 * If no weight exists but confidence requested,
			 * we write:
			 *
			 *   :0.0:confidence
			 *
			 * so the syntax remains valid.
			 */

			if (!wroteWeight) {
				buf.append(":0.0");
			}

			buf.append(":");
			buf.append(format(node.getConfidence()));
		}
	}

	/**
	 * Minimal escaping.
	 * <p>
	 * If labels contain special characters,
	 * wrap them in single quotes.
	 */
	private static String escapeLabel(String s) {

		if (s.matches("[A-Za-z0-9_.-]+"))
			return s;

		return "'" + s.replace("'", "''") + "'";
	}

	private static String format(double value) {

		// avoid locale issues such as comma decimal separators
		return String.format(Locale.US, "%s", value);
	}

	// ---------------------------------------------------------------------

	public static void main(String[] args) {

		String input =
				"((A:0.1:95,B:0.2:90)X:0.3:85,C:0.4)Root:1.0:100;";

		TreeNode root =
				new NewickParser(input).parse();

		System.out.println(write(root, false, false));
		System.out.println(write(root, true, false));
		System.out.println(write(root, true, true));
	}
}