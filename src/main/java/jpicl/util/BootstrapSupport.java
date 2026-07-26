/*
 * BootstrapSupport.java Copyright (C) 2026 Daniel H. Huson
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
import java.util.*;

/**
 * Computes Felsenstein bootstrap support from a set of replicate trees.
 * <p>
 * PICL's tree bootstrap ({@code Boot_type: 1}) does not itself report support
 * values — it writes one re-estimated Newick tree per replicate to the bootstrap
 * output file. Support for a given edge of the main tree is the percentage of
 * replicate trees that contain the same <em>split</em> (bipartition of the taxon
 * set). Splits are compared unrooted, so a replicate rooted differently from the
 * main tree still contributes correctly.
 * <p>
 * {@link #annotate(TreeNode, List)} writes the resulting percentages onto the
 * internal nodes of the main tree via {@link TreeNode#setConfidence(Double)};
 * {@link jpicl.draw.DrawPhylogram} then renders them next to the edges. Trivial
 * splits (pendant edges and the whole-tree split) are left unset.
 */
public final class BootstrapSupport {

	private BootstrapSupport() {
	}

	/**
	 * Reads zero or more {@code ;}-separated Newick trees from a PICL bootstrap
	 * file. A replicate that fails to parse is skipped rather than aborting the
	 * whole set. Non-tree content (e.g. the numeric speciation-time table written
	 * by branch-length bootstrap) yields an empty list.
	 */
	public static List<TreeNode> readReplicateTrees(Path file) throws IOException {
		var trees = new ArrayList<TreeNode>();
		for (var chunk : Files.readString(file).split(";")) {
			var s = chunk.strip();
			if (s.isEmpty() || s.charAt(0) != '(') continue;   // skip blanks / non-Newick lines
			try {
				trees.add(new NewickParser(s).parse());
			} catch (RuntimeException ignored) {
				// tolerate a single malformed replicate
			}
		}
		return trees;
	}

	/**
	 * True if the file's first non-blank character starts a Newick tree, i.e. it
	 * holds replicate trees (tree bootstrap) rather than the numeric table written
	 * by a branch-length bootstrap.
	 */
	public static boolean looksLikeTreeFile(Path file) {
		try {
			var text = Files.readString(file).stripLeading();
			return !text.isEmpty() && text.charAt(0) == '(';
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Annotates each internal edge of {@code mainTree} with bootstrap support in
	 * [0, 100], the percentage of {@code replicates} (restricted to those over the
	 * same taxon set) that contain the corresponding split. Internal nodes get the
	 * value via {@link TreeNode#setConfidence}; trivial splits are left at the
	 * TreeNode default so they are not drawn.
	 *
	 * @return the number of replicate trees actually used.
	 */
	public static int annotate(TreeNode mainTree, List<TreeNode> replicates) {
		var leafSets = new IdentityHashMap<TreeNode, Set<String>>();
		var allLeaves = computeLeafSets(mainTree, leafSets);
		if (allLeaves.size() < 4) return 0;                 // no non-trivial splits
		var ref = Collections.min(allLeaves);               // fixed taxon to orient splits

		// Tally split frequencies across replicates over the same taxon set.
		var counts = new HashMap<String, Integer>();
		int used = 0;
		for (var rep : replicates) {
			var repSets = new IdentityHashMap<TreeNode, Set<String>>();
			var repLeaves = computeLeafSets(rep, repSets);
			if (!repLeaves.equals(allLeaves)) continue;     // different taxa → not comparable
			used++;
			var seen = new HashSet<String>();               // dedupe the root's mirrored split
			for (var side : repSets.values()) {
				var key = splitKey(side, allLeaves, ref);
				if (key != null) seen.add(key);
			}
			for (var k : seen) counts.merge(k, 1, Integer::sum);
		}
		if (used == 0) return 0;

		// Write support onto the main tree's internal nodes.
		for (var entry : leafSets.entrySet()) {
			var node = entry.getKey();
			if (node.isLeaf() || node.getParent() == null) continue;   // skip leaves and root
			var key = splitKey(entry.getValue(), allLeaves, ref);
			if (key == null) continue;                                  // trivial → leave unset
			// The root's two children carry the same split; label only the canonical side.
			if (node.getParent().getParent() == null && entry.getValue().contains(ref)) continue;
			node.setConfidence(100.0 * counts.getOrDefault(key, 0) / used);
		}
		return used;
	}

	/**
	 * Post-order fill of {@code out} with each node's leaf-label set; returns the
	 * tree's full leaf-label set.
	 */
	private static Set<String> computeLeafSets(TreeNode node, Map<TreeNode, Set<String>> out) {
		var set = new TreeSet<String>();
		if (node.isLeaf()) {
			if (node.getLabel() != null && !node.getLabel().isBlank()) set.add(node.getLabel());
		} else {
			for (var c : node.getChildren()) set.addAll(computeLeafSets(c, out));
		}
		out.put(node, set);
		return set;
	}

	/**
	 * Canonical key for the bipartition {@code side | (all - side)}, oriented to the
	 * side not containing {@code ref} so a split has one key regardless of rooting.
	 * Returns {@code null} for trivial splits (a side of size &lt; 2).
	 */
	private static String splitKey(Set<String> side, Set<String> all, String ref) {
		int n = all.size(), k = side.size();
		if (k < 2 || n - k < 2) return null;
		Set<String> canonical;
		if (side.contains(ref)) {
			canonical = new TreeSet<>(all);
			canonical.removeAll(side);
		} else {
			canonical = new TreeSet<>(side);
		}
		return String.join("", canonical);
	}
}
