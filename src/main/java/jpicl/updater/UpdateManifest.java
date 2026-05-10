/*
 * UpdateManifest.java Copyright (C) 2026 Daniel H. Huson
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

public class UpdateManifest {

	private String latestVersion;
	private String minimumCompatibleVersion;

	private PlatformInstaller windows;
	private PlatformInstaller macos;
	private PlatformInstaller linux;

	private String releaseNotesUrl;

	public String getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}

	public String getMinimumCompatibleVersion() {
		return minimumCompatibleVersion;
	}

	public void setMinimumCompatibleVersion(String minimumCompatibleVersion) {
		this.minimumCompatibleVersion = minimumCompatibleVersion;
	}

	public PlatformInstaller getWindows() {
		return windows;
	}

	public void setWindows(PlatformInstaller windows) {
		this.windows = windows;
	}

	public PlatformInstaller getMacos() {
		return macos;
	}

	public void setMacos(PlatformInstaller macos) {
		this.macos = macos;
	}

	public PlatformInstaller getLinux() {
		return linux;
	}

	public void setLinux(PlatformInstaller linux) {
		this.linux = linux;
	}

	public String getReleaseNotesUrl() {
		return releaseNotesUrl;
	}

	public void setReleaseNotesUrl(String releaseNotesUrl) {
		this.releaseNotesUrl = releaseNotesUrl;
	}

	public PlatformInstaller getInstallerForCurrentPlatform() {
		return switch (PlatformDetector.detect()) {
			case WINDOWS -> windows;
			case MACOS -> macos;
			case LINUX -> linux;
		};
	}
}