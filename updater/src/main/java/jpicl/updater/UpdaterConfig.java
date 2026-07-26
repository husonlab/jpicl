/*
 * UpdaterConfig.java Copyright (C) 2026 Daniel H. Huson
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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration object for {@link Updater}. This makes the update reusable
 * across JavaFX applications without hard-wired application names or URLs.
 */
public class UpdaterConfig {
	private final String applicationName;
	private final String currentVersion;
	private final URI manifestUri;
	private final Path downloadDirectory;
	private final Runnable exitAction;
	private final boolean exitAfterLaunchingInstaller;

	private UpdaterConfig(Builder builder) {
		this.applicationName = requireNonBlank(builder.applicationName, "applicationName");
		this.currentVersion = requireNonBlank(builder.currentVersion, "currentVersion");
		this.manifestUri = Objects.requireNonNull(builder.manifestUri, "manifestUri");
		this.downloadDirectory = Objects.requireNonNullElseGet(builder.downloadDirectory, UpdaterConfig::defaultDownloadsDirectory);
		this.exitAction = builder.exitAction;
		this.exitAfterLaunchingInstaller = builder.exitAfterLaunchingInstaller;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Convenience factory for the common case.
	 */
	public static UpdaterConfig configure(String applicationName, String currentVersion, String manifestUrl) {
		return builder()
				.applicationName(applicationName)
				.currentVersion(currentVersion)
				.manifestUri(URI.create(manifestUrl))
				.build();
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public URI getManifestUri() {
		return manifestUri;
	}

	public Path getDownloadDirectory() {
		return downloadDirectory;
	}

	public Runnable getExitAction() {
		return exitAction;
	}

	public boolean isExitAfterLaunchingInstaller() {
		return exitAfterLaunchingInstaller;
	}

	public static Path defaultDownloadsDirectory() {
		var downloads = Path.of(System.getProperty("user.home"), "Downloads");
		return Files.isDirectory(downloads) ? downloads : Path.of(System.getProperty("user.home"));
	}

	private static String requireNonBlank(String value, String name) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	public static class Builder {
		private String applicationName;
		private String currentVersion;
		private URI manifestUri;
		private Path downloadDirectory;
		private Runnable exitAction;
		private boolean exitAfterLaunchingInstaller = true;

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public Builder currentVersion(String currentVersion) {
			this.currentVersion = currentVersion;
			return this;
		}

		public Builder manifestUri(URI manifestUri) {
			this.manifestUri = manifestUri;
			return this;
		}

		public Builder manifestUrl(String manifestUrl) {
			this.manifestUri = URI.create(manifestUrl);
			return this;
		}

		public Builder downloadDirectory(Path downloadDirectory) {
			this.downloadDirectory = downloadDirectory;
			return this;
		}

		/**
		 * Optional cleanup action that is invoked after launching the installer and
		 * before JavaFX/System exit. Use this to close resources, save state, etc.
		 */
		public Builder exitAction(Runnable exitAction) {
			this.exitAction = exitAction;
			return this;
		}

		public Builder exitAfterLaunchingInstaller(boolean exitAfterLaunchingInstaller) {
			this.exitAfterLaunchingInstaller = exitAfterLaunchingInstaller;
			return this;
		}

		public UpdaterConfig build() {
			return new UpdaterConfig(this);
		}
	}
}
