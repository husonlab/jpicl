/*
 * OutputFiles.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Path derivations and collision-avoidance for the files PICL writes
 * (.tre, .trees, .log, .bootstrap, .values) plus the .settings file
 * the controller writes itself before launch.
 *
 * Defensive strategy: rather than overwriting existing files, the
 * caller asks {@link #bumpUntilFree(String, String)} for a coherent
 * set of output paths whose names share the smallest suffix that
 * doesn't collide with anything on disk (data.tre → data-2.tre on
 * second run, data-3.tre on third, …). The .settings file shares the
 * suffix so a single run's outputs always travel together.
 */
public final class OutputFiles {

	// ----- Extension constants -----

	/**
	 * Default extension for the output tree (replaces the alignment extension).
	 */
	public static final String TREE_EXTENSION = ".tre";
	/** Default extension for the settings file (sibling of the output tree). */
	public static final String SETTINGS_EXTENSION = ".settings";
	/** Default extension for the tree-info file (renamed PICL "outtree.tre"). */
	public static final String TREEINFO_EXTENSION = ".trees";
	/** Default extension for the values file (uses the alignment basename). */
	public static final String VALUES_EXTENSION = ".values";
	/** Default extension for the log file (sibling of the output tree). */
	public static final String LOG_EXTENSION = ".log";
	/** Default extension for the bootstrap file (sibling of the output tree). */
	public static final String BOOTSTRAP_EXTENSION = ".bootstrap";

	private OutputFiles() {}

	// =================================================================
	//  Path derivation
	// =================================================================

	/** alignment "/path/foo.phy" → "/path/foo.tre". Empty input → empty result. */
	public static String deriveOutputTreePath(String alignmentPath) {
		if (alignmentPath == null || alignmentPath.isBlank()) return "";
		var p = Paths.get(alignmentPath);
		return p.resolveSibling(stripExtension(p.getFileName().toString()) + TREE_EXTENSION)
				.toString();
	}

	/** outputTree "/path/foo.tre" → "/path/foo.settings". Empty input → "(none)". */
	public static String deriveSettingsPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, SETTINGS_EXTENSION);
	}

	/** outputTree "/path/foo.tre" → "/path/foo.trees". Empty input → "(none)". */
	public static String deriveTreeInfoPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, TREEINFO_EXTENSION);
	}

	/** alignment "/path/foo.phy" → "/path/foo.values". Empty input → "(none)". */
	public static String deriveValuesPath(String alignmentPath) {
		return deriveSiblingPath(alignmentPath, VALUES_EXTENSION);
	}

	/** outputTree "/path/foo.tre" → "/path/foo.log". Empty input → "(none)". */
	public static String deriveLogPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, LOG_EXTENSION);
	}

	/** outputTree "/path/foo.tre" → "/path/foo.bootstrap". Empty input → "(none)". */
	public static String deriveBootstrapPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, BOOTSTRAP_EXTENSION);
	}

	/** Replaces the extension on a path, or returns "(none)" for blank input. */
	public static String deriveSiblingPath(String filePath, String newExtension) {
		if (filePath == null || filePath.isBlank()) return "(none)";
		var p = Paths.get(filePath);
		return p.resolveSibling(stripExtension(p.getFileName().toString()) + newExtension)
				.toString();
	}

	/** "foo.phy" → "foo"; "foo" → "foo"; ".bashrc" → ".bashrc". */
	public static String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return (dot <= 0) ? filename : filename.substring(0, dot);
	}

	// =================================================================
	//  Suffix application + bumping
	// =================================================================

	/**
	 * The set of paths PICL and the controller will write on a single
	 * run, all sharing the same numeric suffix. {@code suffix == 1}
	 * means no rename — the natural names derived from the text fields.
	 * Higher values mean we bumped to data-2.tre, data-3.tre, … because
	 * the natural names collided with files already on disk.
	 *
	 * The .values file uses the alignment basename, the others use the
	 * output-tree basename. They share the suffix so a run's output set
	 * stays coherent (every file from one run carries the same -N).
	 *
	 * Any field may be {@code null} when the corresponding text input
	 * was blank — for example, no alignment selected ⇒ values == null.
	 */
	public record BumpedPaths(int suffix,
							  Path outTree,    // .tre
							  Path treesInfo,  // .trees
							  Path log,        // .log
							  Path bootstrap,  // .bootstrap
							  Path values,     // .values
							  Path settings) { // .settings (controller writes this)

		/** True iff every non-null path in the set is currently free on disk. */
		public boolean allClear() {
			for (var p : new Path[]{outTree, treesInfo, log, bootstrap, values, settings}) {
				if (p != null && Files.exists(p)) return false;
			}
			return true;
		}
	}

	/**
	 * Returns the original filePath unchanged if {@code suffix <= 1},
	 * otherwise inserts {@code -<suffix>} immediately before the
	 * extension. {@code "/data/foo.tre" → "/data/foo-2.tre"}.
	 *
	 * Pass-through for blank input and the "(none)" sentinel so callers
	 * can apply this uniformly to any derived path.
	 */
	public static String applySuffix(String filePath, int suffix) {
		if (suffix <= 1 || filePath == null || filePath.isBlank() || "(none)".equals(filePath))
			return filePath;
		var p = Paths.get(filePath);
		var name = p.getFileName().toString();
		var dot = name.lastIndexOf('.');
		var base = (dot <= 0) ? name : name.substring(0, dot);
		var ext = (dot <= 0) ? "" : name.substring(dot);
		return p.resolveSibling(base + "-" + suffix + ext).toString();
	}

	/**
	 * Walks suffixes 1, 2, 3, … and returns the first {@link BumpedPaths}
	 * where nothing collides on disk. Capped at 999 to surface
	 * pathological situations (e.g. a directory full of stale runs)
	 * rather than spinning forever.
	 *
	 * The returned suffix tells callers whether they need to inform the
	 * user (suffix > 1) or proceed silently (suffix == 1).
	 */
	public static BumpedPaths bumpUntilFree(String alignmentText, String outTreeText) {
		for (int n = 1; n <= 999; n++) {
			var bumped = pathsAtSuffix(alignmentText, outTreeText, n);
			if (bumped.allClear()) return bumped;
		}
		throw new IllegalStateException(
				"Could not find a free output-name suffix within 999 attempts (outTree="
				+ outTreeText + ")");
	}

	/**
	 * The natural (suffix == 1) path set — the names that come straight
	 * from the user's text fields, with no collision avoidance applied.
	 * Use {@link BumpedPaths#allClear()} on the result to decide whether
	 * to prompt the user.
	 */
	public static BumpedPaths naturalPaths(String alignmentText, String outTreeText) {
		return pathsAtSuffix(alignmentText, outTreeText, 1);
	}

	private static BumpedPaths pathsAtSuffix(String alignmentText, String outTreeText, int n) {
		return new BumpedPaths(n,
				toAbsoluteOrNull(applySuffix(outTreeText, n)),
				toAbsoluteOrNull(applySuffix(deriveTreeInfoPath(outTreeText), n)),
				toAbsoluteOrNull(applySuffix(deriveLogPath(outTreeText), n)),
				toAbsoluteOrNull(applySuffix(deriveBootstrapPath(outTreeText), n)),
				toAbsoluteOrNull(applySuffix(deriveValuesPath(alignmentText), n)),
				toAbsoluteOrNull(applySuffix(deriveSettingsPath(outTreeText), n)));
	}

	private static Path toAbsoluteOrNull(String s) {
		if (s == null || s.isBlank() || "(none)".equals(s)) return null;
		return Paths.get(s).toAbsolutePath().normalize();
	}

	// =================================================================
	//  Collision dialog + deletion
	// =================================================================

	/**
	 * What the user chose when told an output file already exists.
	 *
	 * <ul>
	 *   <li>{@link #KEEP_BOTH} — write to a bumped suffix (data-2.tre, …),
	 *       leaving the existing files untouched.</li>
	 *   <li>{@link #REPLACE} — delete the colliding natural-name set and
	 *       write to the original names.</li>
	 *   <li>{@link #STOP} — cancel the run. Returned for both an explicit
	 *       Stop click and a dialog dismissal (close button / ESC).</li>
	 * </ul>
	 */
	public enum CollisionChoice {KEEP_BOTH, STOP, REPLACE}

	/**
	 * Asks the user what to do when the natural-name output set already
	 * exists on disk. Modelled on the macOS Finder copy/replace prompt:
	 * "Keep Both", "Stop", "Replace".
	 *
	 * @param natural the suffix-1 path set the user is about to write to.
	 *                Used both to describe the collision and to list the
	 *                files that {@link #deleteSet(BumpedPaths)} would
	 *                remove on REPLACE.
	 * @param owner   window to centre the modal on (may be null).
	 */
	public static CollisionChoice promptOnCollision(BumpedPaths natural, Window owner) {
		var stop = new ButtonType("Stop", ButtonBar.ButtonData.CANCEL_CLOSE);
		var replace = new ButtonType("Replace", ButtonBar.ButtonData.OTHER);
		var keepBoth = new ButtonType("Keep Both", ButtonBar.ButtonData.OK_DONE);

		var outTreeName = (natural.outTree() == null)
				? "(output)"
				: natural.outTree().getFileName().toString();

		var sb = new StringBuilder();
		sb.append("Files associated with \"").append(outTreeName)
				.append("\" already exist in this location.\n\n")
				.append("Choose Keep Both to write to a new name with a numeric suffix ")
				.append("(e.g. ").append(applySuffix(natural.outTree().toString(), 2))
				.append(").\n\nChoose Replace to delete the existing files and write to ")
				.append("the original names. The following files would be replaced:");
		for (var p : new Path[]{natural.outTree(), natural.treesInfo(), natural.log(),
				natural.bootstrap(), natural.values(), natural.settings()}) {
			if (p != null && Files.exists(p)) sb.append("\n  ").append(p);
		}

		var alert = new Alert(Alert.AlertType.CONFIRMATION, "", replace, stop, keepBoth);
		alert.setHeaderText("Output file exists");
		alert.setTitle("Output file exists");
		alert.setResizable(true);

		// Replace the content with a wrapping Label so long paths show fully.
		var content = new Label(sb.toString());
		content.setWrapText(true);
		content.setMaxWidth(Double.MAX_VALUE);

		var pane = alert.getDialogPane();
		pane.setContent(content);
		pane.setMinWidth(640);
		pane.setPrefWidth(720);

		if (owner != null) alert.initOwner(owner);
		Optional<ButtonType> choice = alert.showAndWait();
		if (choice.isEmpty()) return CollisionChoice.STOP;
		var bt = choice.get();
		if (bt == keepBoth) return CollisionChoice.KEEP_BOTH;
		if (bt == replace) return CollisionChoice.REPLACE;
		return CollisionChoice.STOP;
	}

	/**
	 * Deletes every existing file in the given path set. Missing entries
	 * are ignored, so this is safe to call even when only some of the
	 * natural-name files are present on disk.
	 * <p>
	 * Intended for the REPLACE branch of {@link #promptOnCollision}: the
	 * user has explicitly chosen to overwrite, so we clear the slate
	 * before launching PICL (which appends to some of these files and
	 * would otherwise silently mix runs).
	 */
	public static void deleteSet(BumpedPaths set) throws IOException {
		for (var p : new Path[]{set.outTree(), set.treesInfo(), set.log(),
				set.bootstrap(), set.values(), set.settings()}) {
			if (p != null) Files.deleteIfExists(p);
		}
	}
}
