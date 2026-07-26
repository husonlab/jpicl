/*
 * PlatformDetector.java Copyright (C) 2026 Daniel H. Huson
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

public class PlatformDetector {

	public enum Platform {
		WINDOWS,
		MACOS,
		LINUX
	}

	public enum Architecture {
		X86_64,
		AARCH64,
		OTHER
	}

	public static Platform detect() {
		String os = System.getProperty("os.name", "").toLowerCase();

		if (os.contains("win"))
			return Platform.WINDOWS;
		else if (os.contains("mac"))
			return Platform.MACOS;
		else
			return Platform.LINUX;
	}

	public static Architecture detectArchitecture() {
		String arch = System.getProperty("os.arch", "").toLowerCase();

		if (arch.equals("x86_64") || arch.equals("amd64"))
			return Architecture.X86_64;
		else if (arch.equals("aarch64") || arch.equals("arm64"))
			return Architecture.AARCH64;
		else
			return Architecture.OTHER;
	}

	public static boolean isMacArm64() {
		return detect() == Platform.MACOS && detectArchitecture() == Architecture.AARCH64;
	}

	public static boolean isMacIntel() {
		return detect() == Platform.MACOS && detectArchitecture() == Architecture.X86_64;
	}
}
