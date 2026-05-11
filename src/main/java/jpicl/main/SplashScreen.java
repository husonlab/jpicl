/*
 * SplashScreen.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.animation.PauseTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Objects;

public class SplashScreen {
	public static String SPLASH_IMAGE = "Splash.png";
	public static String VERSION = Version.SHORT_DESCRIPTION;
	public static double VERSION_X = 400;
	public static double VERSION_Y = 20;

	private final Stage stage;

	public SplashScreen() {
		var image = new Image(Objects.requireNonNull(
				SplashScreen.class.getResourceAsStream(SPLASH_IMAGE),
				"Resource not found: " + SPLASH_IMAGE
		));

		var imageView = new ImageView(image);
		imageView.setFitWidth(600);
		imageView.setPreserveRatio(true);

		// --------------------------------------------------------------------
		// Version text
		// --------------------------------------------------------------------

		var versionText = new Text(VERSION);

		versionText.setFont(Font.font("Arial", 11));
		versionText.setManaged(false);
		versionText.setLayoutX(VERSION_X);
		versionText.setLayoutY(VERSION_Y);

		// --------------------------------------------------------------------

		var root = new StackPane();

		root.getChildren().add(imageView);
		root.getChildren().add(versionText);

		root.setPrefSize(imageView.getFitWidth(), imageView.getFitHeight());
		root.setStyle("-fx-background-color: transparent;");
		root.setStyle("-fx-border-width: 1;-fx-border-color: gray;");


		var scene = new Scene(root);
		scene.setFill(Color.TRANSPARENT);

		stage = new Stage(StageStyle.TRANSPARENT);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setAlwaysOnTop(true);

		stage.setOnShown(e -> centerOnScreen(imageView.getFitWidth(), imageView.getFitHeight()));

		root.setOnMouseClicked(e -> hide());

		stage.focusedProperty().addListener((v, oldValue, focused) -> {
			if (!focused) {
				hide();
			}
		});
	}

	/**
	 * Shows the splash screen briefly and then hides it automatically.
	 */
	public void showBriefly(double seconds) {
		show();

		var pause = new PauseTransition(Duration.seconds(seconds));
		pause.setOnFinished(e -> hide());
		pause.play();
	}

	/**
	 * Shows the splash screen until dismissed by the user.
	 */
	public void showUntilDismissed() {
		show();
	}

	public void show() {
		if (!stage.isShowing()) {
			stage.show();
			stage.requestFocus();
		}
	}

	public void hide() {
		if (stage.isShowing()) {
			stage.hide();
		}
	}

	private void centerOnScreen(double width, double height) {
		Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

		stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2.0);
		stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2.0);
	}
}