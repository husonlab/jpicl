/*
 * InstallerLauncher.java Copyright (C) 2026 Daniel H. Huson
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

import java.awt.*;
import java.nio.file.Path;

public class InstallerLauncher {

	public static void launch(Path installer) throws Exception {
		switch (PlatformDetector.detect()) {
			case WINDOWS -> launchWindows(installer);
			case MACOS -> launchMac(installer);
			case LINUX -> launchLinux(installer);
		}
	}

	private static void launchWindows(Path installer) throws Exception {
		String lower = installer.toString().toLowerCase();

		if (lower.endsWith(".msi")) {
			new ProcessBuilder("msiexec", "/i", installer.toString()).start();
		} else {
			Desktop.getDesktop().open(installer.toFile());
		}
	}

	private static void launchMac(Path installer) throws Exception {
		new ProcessBuilder("open", installer.toString()).start();
	}

	private static void launchLinux(Path installer) throws Exception {
		new ProcessBuilder("xdg-open", installer.toString()).start();
	}
}