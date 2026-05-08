/*
 * Window.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.window;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import jpicl.dialog.DialogView;
import jpicl.main.App;

import java.util.Objects;


public class Window {
	final private Stage stage;
	final private DialogView dialogView;

	private Window(Stage stage) {
		var dialogView = new DialogView();
		stage.setScene(new Scene(dialogView.getRoot()));
		stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("PICL-512.png"))));

		this.stage = stage;
		this.dialogView = dialogView;

		var x = javafx.stage.Window.getWindows().stream().mapToDouble(javafx.stage.Window::getX).max().orElse(30.0);
		var y = javafx.stage.Window.getWindows().stream().mapToDouble(javafx.stage.Window::getY).max().orElse(30.0);
		stage.setX(x + 20);
		stage.setY(y + 20);
		var windowId = javafx.stage.Window.getWindows().stream().filter(w -> w.getUserData() instanceof Integer).mapToInt(w -> (Integer) w.getUserData()).max().orElse(0) + 1;
		stage.setUserData(windowId);
		stage.setTitle("PICL" + ((windowId == 1) ? "" : " [" + windowId + "]"));
	}

	public Stage getStage() {
		return stage;
	}

	public DialogView getDialogView() {
		return dialogView;
	}

	public static Window createWindow(Stage stage) {
		return new Window(stage);
	}
}
