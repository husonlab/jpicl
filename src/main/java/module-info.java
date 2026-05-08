module jpicl {
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires javafx.base;

	exports jpicl.main;
	opens jpicl.dialog to javafx.fxml;
	opens jpicl.main to javafx.fxml;

}