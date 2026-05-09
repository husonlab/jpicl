/*
 * NewickParser.java Copyright (C) 2026 Daniel H. Huson
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

/**
 * Simple Newick parser supporting:
 * <p>
 * leaf
 * leaf:0.4
 * leaf:0.4:85
 * (A,B)Inner:0.7:92;
 * <p>
 * Grammar (simplified):
 * <p>
 * subtree := leaf | internal
 * leaf := name? branch?
 * internal := '(' subtree (',' subtree)* ')' name? branch?
 * branch := ':' weight (':' confidence)?
 * <p>
 * Notes:
 * - confidence is optional
 * - node labels are optional
 * - whitespace is ignored
 * - terminating ';' is optional
 */
public class NewickParser {
	private final String input;
	private int pos = 0;

	public NewickParser(String input) {
		this.input = input;
	}

	public TreeNode parse() {
		skipWhitespace();

		TreeNode root = parseSubtree();

		skipWhitespace();

		if (peek() == ';') {
			pos++;
		}

		skipWhitespace();

		if (pos != input.length()) {
			throw error("Unexpected trailing input");
		}

		return root;
	}

	private TreeNode parseSubtree() {
		skipWhitespace();

		TreeNode treeNode;

		if (peek() == '(') {
			treeNode = parseInternal();
		} else {
			treeNode = parseLeaf();
		}

		return treeNode;
	}

	private TreeNode parseInternal() {
		expect('(');

		TreeNode treeNode = new TreeNode();

		while (true) {
			TreeNode child = parseSubtree();
			treeNode.addChild(child);

			skipWhitespace();

			char ch = peek();

			if (ch == ',') {
				pos++;
			} else if (ch == ')') {
				pos++;
				break;
			} else {
				throw error("Expected ',' or ')'");
			}
		}

		skipWhitespace();

		// optional treeNode label
		String label = parseLabel();
		if (!label.isEmpty()) {
			treeNode.setLabel(label);
		}

		parseBranchData(treeNode);

		return treeNode;
	}

	private TreeNode parseLeaf() {
		TreeNode treeNode = new TreeNode();

		String label = parseLabel();

		if (!label.isEmpty()) {
			treeNode.setLabel(label);
		}

		parseBranchData(treeNode);

		return treeNode;
	}

	private void parseBranchData(TreeNode treeNode) {
		skipWhitespace();

		if (peek() == ':') {
			pos++;

			treeNode.setWeight(parseNumber());

			skipWhitespace();

			if (peek() == ':') {
				pos++;

				treeNode.setConfidence(parseNumber());
			}
		}
	}

	private String parseLabel() {
		skipWhitespace();

		int start = pos;

		while (pos < input.length()) {
			char ch = input.charAt(pos);

			if (ch == ':' || ch == ',' || ch == '(' || ch == ')' || ch == ';'
				|| Character.isWhitespace(ch)) {
				break;
			}

			pos++;
		}

		return input.substring(start, pos);
	}

	private double parseNumber() {
		skipWhitespace();

		int start = pos;

		while (pos < input.length()) {
			char ch = input.charAt(pos);

			if (Character.isDigit(ch)
				|| ch == '.'
				|| ch == '-'
				|| ch == '+'
				|| ch == 'e'
				|| ch == 'E') {
				pos++;
			} else {
				break;
			}
		}

		if (start == pos) {
			throw error("Expected number");
		}

		return Double.parseDouble(input.substring(start, pos));
	}

	private void skipWhitespace() {
		while (pos < input.length()
			   && Character.isWhitespace(input.charAt(pos))) {
			pos++;
		}
	}

	private char peek() {
		if (pos >= input.length()) {
			return '\0';
		}
		return input.charAt(pos);
	}

	private void expect(char ch) {
		skipWhitespace();

		if (peek() != ch) {
			throw error("Expected '" + ch + "'");
		}

		pos++;
	}

	private RuntimeException error(String msg) {
		return new RuntimeException(msg + " at position " + pos);
	}

	// ---------------------------------------------------------------------

	public static void main(String[] args) {

		String newick =
				"((A:0.1:95,B:0.2:90)X:0.3:85,C:0.4)Root:1.0:100;";

		TreeNode root = new NewickParser(newick).parse();

		System.out.println(root);
	}
}