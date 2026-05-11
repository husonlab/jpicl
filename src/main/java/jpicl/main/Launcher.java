/*
 * Launcher.java Copyright (C) 2026 Daniel H. Huson
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
package jpicl.main;

/**
 * Bootstrap class that exists solely to work around JavaFX's runtime
 * check. When a JavaFX app is launched directly (its main class
 * extends Application), JavaFX requires javafx.graphics to be on the
 * module path — IntelliJ doesn't set that up by default for a
 * classpath-style project, which produces the "JavaFX runtime
 * components are missing" error.
 * <p>
 * By making this non-Application class the main entry point, we
 * bypass the check; Application.launch() inside JPICL.main() then
 * works fine because the JavaFX classes are loadable on the
 * classpath.
 */
public class Launcher {
	public static void main(String[] args) {
		Main.main(args);
	}
}