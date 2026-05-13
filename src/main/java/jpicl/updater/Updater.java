/*
 * Updater.java Copyright (C) 2026 Daniel H. Huson
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
import javafx.beans.property.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Updater {

	private final String currentVersion;
	private final URI manifestUri;
	private final Path downloadDirectory;

	private final HttpClient httpClient;

	private final ExecutorService executorService =
			Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "UpdaterThread");
				t.setDaemon(true);
				return t;
			});

	private final BooleanProperty checking =
			new SimpleBooleanProperty(false);

	private final BooleanProperty updateAvailable =
			new SimpleBooleanProperty(false);

	private final BooleanProperty downloading =
			new SimpleBooleanProperty(false);

	private final DoubleProperty downloadProgress =
			new SimpleDoubleProperty(-1);

	private final StringProperty statusMessage =
			new SimpleStringProperty("");

	private volatile UpdateManifest availableUpdate;

	public Updater(String currentVersion, URI manifestUri, Path downloadDirectory) {

		this.currentVersion = Objects.requireNonNull(currentVersion);
		this.manifestUri = Objects.requireNonNull(manifestUri);
		this.downloadDirectory = Objects.requireNonNull(downloadDirectory);

		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20))
				.build();
	}

	/**
	 * Silently checks whether an update is available.
	 * This method never blocks the JavaFX UI thread.
	 */
	public void checkForUpdatesSilently() {

		if (checking.get())
			return;

		checking.set(true);

		CompletableFuture
				.supplyAsync(this::downloadManifest, executorService).thenAccept(manifest -> {
					try {
						if (manifest == null)
							return;
						if (VersionComparator.isNewer(manifest.getLatestVersion(), currentVersion)) {
							availableUpdate = manifest;
							Platform.runLater(() -> {
								updateAvailable.set(true);
								statusMessage.set("Update available: " + manifest.getLatestVersion());
							});
						} else {
							Platform.runLater(() -> {
								updateAvailable.set(false);
								statusMessage.set(
										"Application is up to date");
							});
						}
					} finally {
						Platform.runLater(() -> checking.set(false));
					}
				})
				.exceptionally(ex -> {
					ex.printStackTrace();
					Platform.runLater(() -> {
						checking.set(false);
						statusMessage.set("Update check failed: " + ex.getMessage());
					});
					return null;
				});
	}

	/**
	 * Downloads and parses the update manifest.
	 */
	private UpdateManifest downloadManifest() {
		try {
			HttpRequest request = HttpRequest.newBuilder(manifestUri).header("Accept", "application/json").GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new IOException("Manifest request failed: HTTP " + response.statusCode());
			}
			return JsonUtils.MAPPER.readValue(response.body(), UpdateManifest.class);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Downloads the installer for the current platform.
	 */
	public CompletableFuture<Path> downloadUpdate() {
		if (availableUpdate == null) {
			throw new IllegalStateException("No update available");
		}
		if (downloading.get()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Already downloading"));
		}

		downloading.set(true);
		downloadProgress.set(-1);

		return CompletableFuture.supplyAsync(() -> {

			try {
				PlatformInstaller installer = availableUpdate.getInstallerForCurrentPlatform();
				if (installer == null) {
					throw new IllegalStateException("No installer available for current platform");
				}
				Files.createDirectories(downloadDirectory);
				Platform.runLater(() -> statusMessage.set("Downloading update..."));
				Path downloaded = InstallerDownloader.download(installer, downloadDirectory);
				Platform.runLater(() -> {
					downloadProgress.set(1.0);
					statusMessage.set("Download complete");
				});
				return downloaded;
			} catch (Exception ex) {
				Platform.runLater(() -> statusMessage.set("Download failed: " + ex.getMessage()));
				throw new RuntimeException(ex);
			} finally {
				Platform.runLater(() -> downloading.set(false));
			}
		}, executorService);
	}

	/**
	 * Launches the installer and exits the application.
	 */
	public void installAndExit(Path installer) {
		Objects.requireNonNull(installer);

		try {
			statusMessage.set("Launching installer...");
			InstallerLauncher.launch(installer);
			shutdown();
			Platform.exit();
			System.exit(0);

		} catch (Exception ex) {
			ex.printStackTrace();
			statusMessage.set("Failed to launch installer: " + ex.getMessage());
		}
	}

	/**
	 * Stops all updater background threads.
	 */
	public void shutdown() {
		executorService.shutdownNow();
	}

	public Optional<UpdateManifest> getAvailableUpdate() {
		return Optional.ofNullable(availableUpdate);
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public ReadOnlyBooleanProperty updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isChecking() {
		return checking.get();
	}

	public ReadOnlyBooleanProperty checkingProperty() {
		return checking;
	}

	public boolean isDownloading() {
		return downloading.get();
	}

	public ReadOnlyBooleanProperty downloadingProperty() {
		return downloading;
	}

	public double getDownloadProgress() {
		return downloadProgress.get();
	}

	public ReadOnlyDoubleProperty downloadProgressProperty() {
		return downloadProgress;
	}

	public String getStatusMessage() {
		return statusMessage.get();
	}

	public ReadOnlyStringProperty statusMessageProperty() {
		return statusMessage;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}
}