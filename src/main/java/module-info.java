module jpicl {
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires javafx.base;
	requires jloda_fx;

	exports jpicl.main;
	opens jpicl.dialog to javafx.fxml;
}