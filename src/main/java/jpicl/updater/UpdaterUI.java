/*
 * UpdaterUI.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.updater;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Drives the user-facing "Check for Updates…" flow on top of a
 * background-only {@link Updater}. One entry point ({@link #checkAndPrompt()})
 * kicks off a silent check and then presents one of three modal
 * dialogs depending on the outcome:
 *
 * <ul>
 *   <li><b>Up to date</b> — the manifest's {@code latestVersion} is
 *       not newer than the running version.</li>
 *   <li><b>Update available</b> — confirmation dialog naming the new
 *       version, the current version, and the target directory; on
 *       OK, the installer downloads asynchronously, and a second
 *       dialog asks whether to run it.</li>
 *   <li><b>Update check failed</b> — surfaces the {@link Updater}'s
 *       status message verbatim (network error, manifest parse error,
 *       etc.).</li>
 * </ul>
 * <p>
 * The owner Window is resolved lazily via a {@link Supplier} so the
 * UI can be constructed before the JavaFX scene is attached to a
 * stage. Pass {@code () -> someStage.getScene().getWindow()} or
 * similar.
 */
public class UpdaterUI {

	private final Updater updater;
	private final Supplier<Window> ownerSupplier;

	public UpdaterUI(Updater updater, Supplier<Window> ownerSupplier) {
		this.updater = updater;
		this.ownerSupplier = ownerSupplier;
	}

	/**
	 * Returns {@code ~/Downloads} if it exists, falling back to the
	 * user home directory. Cross-platform: on macOS, Linux, and
	 * Windows alike the user's downloads folder is conventionally
	 * named {@code Downloads} directly under the home dir.
	 */
	public static Path defaultDownloadsDirectory() {
		var downloads = Path.of(System.getProperty("user.home"), "Downloads");
		return Files.isDirectory(downloads)
				? downloads
				: Path.of(System.getProperty("user.home"));
	}

	// =================================================================
	//  Public API — wire to your menu item
	// =================================================================

	/**
	 * Entry point invoked when the user selects "Check for Updates…".
	 * Starts a silent check (or rides along with one already in
	 * progress) and shows the result dialog when it completes.
	 */
	public void checkAndPrompt() {
		// One-shot listener — fires the moment the check transitions
		// true → false, then removes itself. Works whether we just
		// triggered the check or it was already running.
		ChangeListener<Boolean> listener = new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> obs, Boolean was, Boolean is) {
				if (Boolean.TRUE.equals(was) && Boolean.FALSE.equals(is)) {
					updater.checkingProperty().removeListener(this);
					Platform.runLater(UpdaterUI.this::showResult);
				}
			}
		};
		updater.checkingProperty().addListener(listener);

		if (!updater.isChecking()) {
			updater.checkForUpdatesSilently();
		}
	}

	// =================================================================
	//  Result branching
	// =================================================================

	private void showResult() {
		// The Updater's statusMessageProperty has three sentinel
		// prefixes set inside Updater.checkForUpdatesSilently:
		//   "Update available: …"   — happy path (also availableUpdate is set)
		//   "Application is up to date"
		//   "Update check failed: …"
		// Detecting the error case by message prefix is brittle, but
		// the alternative — adding a CheckResult enum to Updater —
		// would mean reshaping Updater's public API.
		var status = updater.getStatusMessage();
		if (status != null && status.startsWith("Update check failed")) {
			showError(status);
			return;
		}
		updater.getAvailableUpdate().ifPresentOrElse(
				this::promptToDownload,
				this::showUpToDate);
	}

	private void showUpToDate() {
		var alert = new Alert(Alert.AlertType.INFORMATION,
				"You are running JPICL " + updater.getCurrentVersion() + ".",
				ButtonType.OK);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("This is the latest version.");
		attachOwner(alert);
		alert.showAndWait();
	}

	private void showError(String message) {
		var alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("Update check failed");
		attachOwner(alert);
		alert.showAndWait();
	}

	private void promptToDownload(UpdateManifest manifest) {
		var target = defaultDownloadsDirectory();
		var msg = "JPICL " + manifest.getLatestVersion() + " is available.\n"
				  + "You are currently running JPICL " + updater.getCurrentVersion() + ".\n\n"
				  + "Save the installer to:\n  " + target + "\n\n"
				  + "Download now?";
		var alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
				ButtonType.OK, ButtonType.CANCEL);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("A new version is available");
		attachOwner(alert);

		var choice = alert.showAndWait();
		if (choice.isPresent() && choice.get() == ButtonType.OK) {
			startDownload();
		}
	}

	private void startDownload() {
		// downloadUpdate returns a CompletableFuture; route both
		// outcomes through the FX thread before touching the UI.
		updater.downloadUpdate().whenComplete((path, error) ->
				Platform.runLater(() -> {
					if (error != null) {
						var cause = error.getCause() != null ? error.getCause() : error;
						showError("Download failed: " + cause.getMessage());
					} else {
						promptToRun(path);
					}
				}));
	}

	private void promptToRun(Path installer) {
		var msg = "Installer saved to:\n  " + installer + "\n\nRun the installer now?\n"
				  + "(JPICL will exit when the installer launches.)";
		var alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
				ButtonType.OK, ButtonType.CANCEL);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("Download complete");
		attachOwner(alert);

		var choice = alert.showAndWait();
		if (choice.isPresent() && choice.get() == ButtonType.OK) {
			updater.installAndExit(installer);
		}
	}

	private void attachOwner(Alert alert) {
		var owner = ownerSupplier == null ? null : ownerSupplier.get();
		if (owner != null) alert.initOwner(owner);
	}
}
