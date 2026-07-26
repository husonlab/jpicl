/*
 * UpdateServiceGitHub.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Window;
import jpicl.main.Version;
import jpicl.main.UpdateService;

public class UpdateServiceGitHub implements UpdateService {
	private final SimpleBooleanProperty disabled = new SimpleBooleanProperty(false);

	@Override
	public void checkForUpdates(Window owner) {
		var updater = new Updater(UpdaterConfig.builder()
				.applicationName(Version.NAME)
				.currentVersion(Version.VERSION)
				.manifestUrl(Version.UPDATE_MANIFEST_URL)
				.build());
		var updaterUI = new UpdaterUI(updater, () -> owner);
		updaterUI.checkAndPrompt();
	}

	@Override
	public ReadOnlyBooleanProperty disabledProperty() {
		return disabled;
	}
}
