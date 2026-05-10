module jpicl {
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires javafx.base;
	requires com.fasterxml.jackson.databind;
	requires java.net.http;
	requires java.desktop;

	exports jpicl.main;
	opens jpicl.dialog to javafx.fxml;
	opens jpicl.main to javafx.fxml;

}