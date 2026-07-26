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

/**
 * JSON model for the release manifest.
 * <p>
 * Current manifest format:
 *
 * <pre>
 * {
 *   "latestVersion": "1.0.1",
 *   "releaseNotesUrl": "https://github.com/.../releases/tag/v1.0.1",
 *   "macosArm64": { "installerUrl": "...dmg", "sha256": "..." },
 *   "macosIntel": { "installerUrl": "...dmg", "sha256": "..." },
 *   "linuxDeb": { "installerUrl": "...deb", "sha256": "..." },
 *   "linuxTarGz": { "installerUrl": "...tar.gz", "sha256": "..." },
 *   "windowsMsi": { "installerUrl": "...msi", "sha256": "..." }
 * }
 * </pre>
 * <p>
 * The legacy fields {@code macos}, {@code linux}, and {@code windows} are also
 * accepted so that older manifests remain readable during the transition.
 */
public class UpdateManifest {
	private String latestVersion;
	private String releaseNotesUrl;

	// Current manifest fields:
	private PlatformInstaller macosArm64;
	private PlatformInstaller macosIntel;
	private PlatformInstaller linuxDeb;
	private PlatformInstaller linuxTarGz;
	private PlatformInstaller windowsMsi;

	// Legacy manifest fields:
	private PlatformInstaller macos;
	private PlatformInstaller linux;
	private PlatformInstaller windows;

	public String getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}

	public String getReleaseNotesUrl() {
		return releaseNotesUrl;
	}

	public void setReleaseNotesUrl(String releaseNotesUrl) {
		this.releaseNotesUrl = releaseNotesUrl;
	}

	public PlatformInstaller getMacosArm64() {
		return macosArm64;
	}

	public void setMacosArm64(PlatformInstaller macosArm64) {
		this.macosArm64 = macosArm64;
	}

	public PlatformInstaller getMacosIntel() {
		return macosIntel;
	}

	public void setMacosIntel(PlatformInstaller macosIntel) {
		this.macosIntel = macosIntel;
	}

	public PlatformInstaller getLinuxDeb() {
		return linuxDeb;
	}

	public void setLinuxDeb(PlatformInstaller linuxDeb) {
		this.linuxDeb = linuxDeb;
	}

	public PlatformInstaller getLinuxTarGz() {
		return linuxTarGz;
	}

	public void setLinuxTarGz(PlatformInstaller linuxTarGz) {
		this.linuxTarGz = linuxTarGz;
	}

	public PlatformInstaller getWindowsMsi() {
		return windowsMsi;
	}

	public void setWindowsMsi(PlatformInstaller windowsMsi) {
		this.windowsMsi = windowsMsi;
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

	public PlatformInstaller getWindows() {
		return windows;
	}

	public void setWindows(PlatformInstaller windows) {
		this.windows = windows;
	}

	public PlatformInstaller getInstallerForCurrentPlatform() {
		return switch (PlatformDetector.detect()) {
			case WINDOWS -> firstNonNull(windowsMsi, windows);
			case LINUX -> firstNonNull(linuxTarGz, linuxDeb, linux);
			case MACOS -> switch (PlatformDetector.detectArchitecture()) {
				case AARCH64 -> firstNonNull(macosArm64, macos, macosIntel);
				case X86_64 -> firstNonNull(macosIntel, macos, macosArm64);
				case OTHER -> firstNonNull(macosArm64, macos, macosIntel);
			};
		};
	}

	private static PlatformInstaller firstNonNull(PlatformInstaller... installers) {
		for (var installer : installers) {
			if (installer != null)
				return installer;
		}
		return null;
	}
}
