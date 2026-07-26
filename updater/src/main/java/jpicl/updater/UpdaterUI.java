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
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * User-facing JavaFX update-check dialogs on top of {@link Updater}.
 */
public class UpdaterUI {
	private final Updater updater;
	private final Supplier<Window> ownerSupplier;

	public UpdaterUI(Updater updater, Supplier<Window> ownerSupplier) {
		this.updater = updater;
		this.ownerSupplier = ownerSupplier;
	}

	public static Path defaultDownloadsDirectory() {
		return UpdaterConfig.defaultDownloadsDirectory();
	}

	/**
	 * Entry point for a "Check for Updates..." menu item.
	 */
	public void checkAndPrompt() {
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

		if (!updater.isChecking())
			updater.checkForUpdatesSilently();
	}

	private void showResult() {
		var status = updater.getStatusMessage();
		if (status != null && status.startsWith("Update check failed")) {
			showError(status);
			return;
		}
		updater.getAvailableUpdate().ifPresentOrElse(this::promptToDownload, this::showUpToDate);
	}

	private void showUpToDate() {
		var app = updater.getApplicationName();
		var alert = new Alert(Alert.AlertType.INFORMATION,
				"You are running " + app + " " + updater.getCurrentVersion() + ".",
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
		var app = updater.getApplicationName();
		var target = updater.getConfig().getDownloadDirectory();
		var msg = app + " " + manifest.getLatestVersion() + " is available.\n"
				  + "You are currently running " + app + " " + updater.getCurrentVersion() + ".\n\n"
				  + "Save the installer to:\n  " + target + "\n\n"
				  + "Download now?";
		var alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("A new version is available");
		attachOwner(alert);

		var choice = alert.showAndWait();
		if (choice.isPresent() && choice.get() == ButtonType.OK)
			startDownload();
	}

	private void startDownload() {
		var progressAlert = createDownloadProgressAlert();
		var progressBar = (ProgressBar) progressAlert.getDialogPane().lookup("#downloadProgressBar");
		var statusLabel = (Label) progressAlert.getDialogPane().lookup("#downloadStatusLabel");

		var progressBinding = updater.downloadProgressProperty();
		var statusBinding = updater.statusMessageProperty();

		progressBar.progressProperty().bind(progressBinding);
		statusLabel.textProperty().bind(statusBinding);

		var cancelButton = progressAlert.getDialogPane().lookupButton(ButtonType.CANCEL);
		if (cancelButton != null) {
			cancelButton.setDisable(false);
			cancelButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
				e.consume();
				updater.cancelDownload();
				statusLabel.textProperty().unbind();
				statusLabel.setText("Cancelling download...");
				cancelButton.setDisable(true);
			});
		}

		updater.downloadUpdate().whenComplete((path, error) -> Platform.runLater(() -> {
			progressBar.progressProperty().unbind();
			statusLabel.textProperty().unbind();
			progressAlert.close();

			if (error != null) {
				var cause = rootCause(error);
				if (cause instanceof UserCanceledException)
					return;
				showError("Download failed: " + cause.getMessage());
			} else {
				promptToRun(path);
			}
		}));

		progressAlert.show();
	}

	private Alert createDownloadProgressAlert() {
		var progressBar = new ProgressBar();
		progressBar.setId("downloadProgressBar");
		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressBar.setProgress(-1);

		var statusLabel = new Label("Starting download...");
		statusLabel.setId("downloadStatusLabel");
		statusLabel.setWrapText(true);
		statusLabel.setMaxWidth(Double.MAX_VALUE);

		var box = new VBox(10, statusLabel, progressBar);
		box.setPadding(new Insets(5, 0, 0, 0));
		box.setMinWidth(420);

		var alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("Downloading update");
		alert.getDialogPane().setContent(box);
		alert.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
		attachOwner(alert);
		return alert;
	}

	private void promptToRun(Path installer) {
		var msg = "Installer saved to:\n  " + installer + "\n\nRun the installer now?\n"
				  + "(" + updater.getApplicationName() + " will exit when the installer launches.)";
		var alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
		alert.setTitle("Check for Updates");
		alert.setHeaderText("Download complete");
		attachOwner(alert);

		var choice = alert.showAndWait();
		if (choice.isPresent() && choice.get() == ButtonType.OK)
			updater.installAndExit(installer);
	}

	private static Throwable rootCause(Throwable throwable) {
		var t = throwable;
		while (t.getCause() != null)
			t = t.getCause();
		return t;
	}

	private void attachOwner(Alert alert) {
		var owner = ownerSupplier == null ? null : ownerSupplier.get();
		if (owner != null)
			alert.initOwner(owner);
	}
}
