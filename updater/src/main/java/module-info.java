module updater {
	requires jpicl;
	requires java.net.http;
	requires java.datatransfer;
	requires javafx.base;
	requires javafx.controls;
	requires java.desktop;
	requires com.fasterxml.jackson.databind;

	provides jpicl.main.UpdateService with jpicl.updater.UpdateServiceGitHub;
	exports jpicl.updater;
}