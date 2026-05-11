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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Window;

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
	//  Confirmation dialog
	// =================================================================

	/**
	 * Asks the user whether to write to a renamed output set because
	 * the natural names already exist. Returns true on OK.
	 *
	 * The user can still abort by clicking Cancel — we never silently
	 * overwrite, and never silently rename.
	 */
	public static boolean confirmRename(String originalOutTree, BumpedPaths bumped, Window owner) {
		var msg = "The output tree already exists:\n  " + originalOutTree + "\n\n"
				  + "Write to this name instead?\n  " + bumped.outTree() + "\n\n"
				  + "Companion files (.trees, .log, .bootstrap, .values, .settings) "
				  + "will share the same -" + bumped.suffix() + " suffix.";

		var alert = new Alert(Alert.AlertType.CONFIRMATION, "",
				ButtonType.OK, ButtonType.CANCEL);
		alert.setHeaderText("Output file exists");
		alert.setTitle("Use a new filename?");
		alert.setResizable(true);

		// Replace the content with our own wrapping Label. The default
		// Alert content node has a baked-in width that truncates long
		// absolute file paths; this lets us size the dialog properly.
		var content = new Label(msg);
		content.setWrapText(true);
		content.setMaxWidth(Double.MAX_VALUE);

		var pane = alert.getDialogPane();
		pane.setContent(content);
		pane.setMinWidth(640);
		pane.setPrefWidth(720);

		if (owner != null) alert.initOwner(owner);
		Optional<ButtonType> choice = alert.showAndWait();
		return choice.isPresent() && choice.get() == ButtonType.OK;
	}
}
