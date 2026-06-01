/*
 * DialogPresenter.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.dialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import jpicl.draw.DrawPhylogram;
import jpicl.main.SplashScreen;
import jpicl.main.UpdateService;
import jpicl.main.Version;
import jpicl.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * All UI wiring, event handlers, and run-time state for the PICL
 * settings dialog.The constructor performs every
 * configure-* call so that by the time it returns the dialog is fully
 * live and bound.
 * <p>
 * The {@link Settings} instance is the source of truth. The UI is
 * populated from it ({@link #applyToUi(Settings)}) and edits are
 * pushed back ({@link #pullFromUi(Settings)}) on demand.
 */
public class DialogPresenter {

	// -----------------------------------------------------------------
	//  Collaborators + state
	// -----------------------------------------------------------------

	private final DialogController controller;

	private final Settings settings = new Settings();
	private final Random random = new Random();
	private File lastSettingsFile;

	/**
	 * True while a PICL process is running. Drives Run/Stop button enable state.
	 */
	private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);
	/**
	 * The currently running process, or null.
	 */
	private Process currentProcess;

	/**
	 * Writer to the per-run log file; null when no run is in flight.
	 */
	private BufferedWriter logFileWriter;

	/**
	 * Last tree file path loaded into the Output tab (null if none).
	 */
	private Path lastTreeFile;

	/**
	 * The output-tree path we last derived automatically from the alignment.
	 * If the user-visible value still equals this, alignment changes will
	 * re-derive it; if the user has typed something else, we leave it alone.
	 */
	private String lastAutoDerivedOutputTree = "";

	/**
	 * Tree-info path written by picl in the most recent run; null if no run yet.
	 */
	private Path pendingTreeInfoPath;

	/**
	 * Output tree (.tre) path written by picl in the most recent run; null if no run yet.
	 */
	private Path pendingOutTreePath;

	/**
	 * Last successfully parsed tree root, retained so we can redraw on resize.
	 */
	private TreeNode lastDrawnTreeRoot;
	// -----------------------------------------------------------------
	//  Construction — does ALL the wiring
	// -----------------------------------------------------------------

	public DialogPresenter(DialogController controller) {
		this.controller = controller;

		configureChoiceBoxes();
		configureTable();
		configureEnableDisableBindings();
		configureCountLabels();
		configureFilesSection();
		configureOutputTab();
		configureTreeTab();
		configureMenuBar();
		wireButtonHandlers();

		applyToUi(settings);  // populate with defaults
		controller.getStatusLabel().setText("Ready");
	}

	public Settings getSettings() {
		return settings;
	}

	// =================================================================
	//  Menu bar wiring
	// =================================================================

	private void configureMenuBar() {
		var menuBar = controller.getMenuBar();

		// ----- File -----
		controller.getNewMenuItem().setOnAction(e -> onNew());
		controller.getOpenMenuItem().setOnAction(e -> browseForFile(
				controller.getAlignmentFileTextField(), "Multiple sequence alignment",
				"*.phy", "*.phylip", "*.fasta", "*.fna"));
		controller.getExportSettingsMenuItem().setOnAction(this::onSaveSettings);
		controller.getImportSettingsMenuItem().setOnAction(this::onLoadSettings);
		controller.getPrintMenuItem().setOnAction(e -> onPrint());
		controller.getPageSetupMenuItem().setOnAction(e -> onPageSetup());
		controller.getCloseMenuItem().setOnAction(e -> {
			var stage = (Stage) menuBar.getScene().getWindow();
			stage.close();
		});
		controller.getQuitMenuItem().setOnAction(e -> Platform.exit());

		// ----- Edit -----
		// Forward to the focused TextInputControl. JavaFX text fields
		// already handle the keyboard shortcuts natively; the menu items
		// give the user a discoverable, click-driven path.
		controller.getUndoMenuItem().setOnAction(e -> onFocusedText(TextInputControl::undo));
		controller.getRedoMenuItem().setOnAction(e -> onFocusedText(TextInputControl::redo));
		controller.getCutMenuItem().setOnAction(e -> onFocusedText(TextInputControl::cut));
		controller.getCopyMenuItem().setOnAction(e -> onFocusedText(TextInputControl::copy));
		controller.getPasteMenuItem().setOnAction(e -> onFocusedText(TextInputControl::paste));
		controller.getDeleteMenuItem().setOnAction(e -> onFocusedText(TextInputControl::deleteNextChar));

		// ----- View -----
		// Full screen and Dark mode hooks need a Scene, which isn't
		// available until the controls are attached to a window. Defer.
		Platform.runLater(this::installSceneDependentMenuBindings);

		// Tab radio items: keep the radio menu and the TabPane in sync.
		var mainTabPane = controller.getMainTabPane();
		var settingsTab = controller.getSettingsTab();
		var logTab = controller.getLogTab();
		var outputTab = controller.getOutputTab();
		var treeTab = controller.getTreeTab();
		var settingsTabMenuItem = controller.getSettingsTabMenuItem();
		var logTabMenuItem = controller.getLogTabMenuItem();
		var outputTabMenuItem = controller.getOutputTabMenuItem();
		var treeTabMenuItem = controller.getTreeTabMenuItem();

		settingsTabMenuItem.setOnAction(e -> mainTabPane.getSelectionModel().select(settingsTab));
		logTabMenuItem.setOnAction(e -> mainTabPane.getSelectionModel().select(logTab));
		outputTabMenuItem.setOnAction(e -> mainTabPane.getSelectionModel().select(outputTab));
		treeTabMenuItem.setOnAction(e -> mainTabPane.getSelectionModel().select(treeTab));

		mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
			if (newTab == settingsTab) settingsTabMenuItem.setSelected(true);
			else if (newTab == logTab) logTabMenuItem.setSelected(true);
			else if (newTab == outputTab) outputTabMenuItem.setSelected(true);
			else if (newTab == treeTab) treeTabMenuItem.setSelected(true);
		});
		settingsTabMenuItem.setSelected(true);

		var updaterService = UpdateService.get();
		controller.getCheckForUpdatesMenuItem().setOnAction(e -> updaterService.checkForUpdates(menuBar.getScene() == null ? null : menuBar.getScene().getWindow()));
		controller.getCheckForUpdatesMenuItem().disableProperty().bind(Bindings.size(Window.getWindows()).greaterThan(1));

		controller.getAboutMenuItem().setOnAction(e -> {
			new SplashScreen().showUntilDismissed();
		});

		controller.getOpenGitHubMenuItem().setOnAction(e -> WebBrowser.openURL(Version.GITHUB_PAGE));

		controller.getCopyImageMenuItem().setOnAction(e -> {
			var params = new SnapshotParameters();
			var image = controller.getTreeCanvasPane().snapshot(params, null);
			var content = new ClipboardContent();
			content.putImage(image);
			Clipboard.getSystemClipboard().setContent(content);
		});
		controller.getCopyImageMenuItem().disableProperty().bind(controller.getOutputTextArea().textProperty().isEmpty());

		controller.getCopyTreeMenuItem().setOnAction(this::onCopyTree);
		controller.getCopyTreeMenuItem().disableProperty().bind(controller.getOutputTextArea().textProperty().isEmpty());
	}

	/**
	 * Bindings that need a live Scene (full-screen, dark mode, windows list).
	 */
	private void installSceneDependentMenuBindings() {
		var menuBar = controller.getMenuBar();
		var scene = menuBar.getScene();
		if (scene == null) return;
		var stage = (Stage) scene.getWindow();
		if (stage == null) return;

		var fullScreenMenuItem = controller.getFullScreenMenuItem();
		fullScreenMenuItem.setSelected(stage.isFullScreen());
		fullScreenMenuItem.setOnAction(e -> stage.setFullScreen(fullScreenMenuItem.isSelected()));
		stage.fullScreenProperty().addListener((obs, was, now) -> fullScreenMenuItem.setSelected(now));

		var darkModeMenuItem = controller.getDarkModeMenuItem();
		darkModeMenuItem.setOnAction(e -> applyDarkMode(scene, darkModeMenuItem.isSelected()));

		// Windows section at the bottom of View.
		Window.getWindows().addListener((ListChangeListener<Window>) c ->
				Platform.runLater(this::rebuildWindowsSection));
		Platform.runLater(this::rebuildWindowsSection);
	}

	/**
	 * Marker placed in {@code MenuItem.userData} on every item the
	 * windows-section rebuild owns. Used so we can wipe and recreate the
	 * section on each change without disturbing the static menu items
	 * above (Full Screen, Dark Mode, the tabs toggle group, etc.).
	 */
	private static final Object WINDOWS_SECTION_MARKER = new Object();

	private void rebuildWindowsSection() {
		var windowMenu = controller.getWindowMenu();
		windowMenu.getItems().removeIf(item -> item.getUserData() == WINDOWS_SECTION_MARKER);

		boolean addedSeparator = false;
		for (var w : Window.getWindows()) {
			if (!(w instanceof Stage stage)) continue;
			if (!stage.isShowing()) continue;
			var title = stage.getTitle();
			if (title == null || title.isBlank()) continue;

			if (!addedSeparator) {
				var sep = new SeparatorMenuItem();
				sep.setUserData(WINDOWS_SECTION_MARKER);
				windowMenu.getItems().add(sep);
				addedSeparator = true;
			}

			var item = new MenuItem();
			item.textProperty().bind(stage.titleProperty());
			item.setUserData(WINDOWS_SECTION_MARKER);
			item.setOnAction(e -> {
				stage.toFront();
				stage.requestFocus();
			});
			Platform.runLater(() -> windowMenu.getItems().add(item));
		}
	}

	private static void applyDarkMode(Scene scene, boolean dark) {
		var root = scene.getRoot();
		if (dark) {
			scene.getStylesheets().add(Objects.requireNonNull(DialogPresenter.class.getResource("Dark.css")).toExternalForm());
		} else {
			scene.getStylesheets().remove(Objects.requireNonNull(DialogPresenter.class.getResource("Dark.css")).toExternalForm());
		}
	}

	private void onFocusedText(Consumer<TextInputControl> action) {
		var scene = controller.getMenuBar().getScene();
		if (scene == null) return;
		Node owner = scene.getFocusOwner();
		if (owner instanceof TextInputControl tic) action.accept(tic);
	}

	private void onNew() {
		jpicl.window.Window.createWindow(new Stage()).getStage().show();
	}

	private void onPrint() {
		var node = nodeToPrint();
		if (node == null) {
			controller.getStatusLabel().setText("Nothing to print.");
			return;
		}
		var job = PrinterJob.createPrinterJob();
		if (job == null) {
			controller.getStatusLabel().setText("No printer available.");
			return;
		}
		var stage = controller.getMenuBar().getScene().getWindow();
		if (job.showPrintDialog(stage) && job.printPage(node)) {
			job.endJob();
			controller.getStatusLabel().setText("Printed.");
		}
	}

	private void onPageSetup() {
		var job = PrinterJob.createPrinterJob();
		if (job == null) {
			controller.getStatusLabel().setText("No printer available.");
			return;
		}
		job.showPageSetupDialog(controller.getMenuBar().getScene().getWindow());
	}

	private Node nodeToPrint() {
		var sel = controller.getMainTabPane().getSelectionModel().getSelectedItem();
		if (sel == controller.getLogTab()) return controller.getLogView();
		if (sel == controller.getOutputTab()) return controller.getOutputTextArea();
		return controller.getMainTabPane();
	}

	// =================================================================
	//  Files section (alignment ⇒ output-tree ⇒ derived-paths chain)
	// =================================================================

	private void configureFilesSection() {
		var alignmentFileTextField = controller.getAlignmentFileTextField();
		var outTreeFileTextField = controller.getOutTreeFileTextField();

		// When the alignment changes, re-derive the output tree path —
		// but only if the user hasn't customized it (i.e. it still matches
		// whatever we last auto-derived). Also refresh the Values label.
		alignmentFileTextField.textProperty().addListener((obs, oldVal, newVal) -> {
			var derived = OutputFiles.deriveOutputTreePath(newVal);
			var current = outTreeFileTextField.getText();
			if (current == null || current.isBlank()
				|| current.equals(lastAutoDerivedOutputTree)) {
				outTreeFileTextField.setText(derived);
			}
			lastAutoDerivedOutputTree = derived;
			controller.getValuesPathLabel().setText(OutputFiles.deriveValuesPath(newVal));
		});

		// Settings, trees, log, and bootstrap labels all track the
		// output-tree path live.
		outTreeFileTextField.textProperty().addListener((obs, oldVal, newVal) -> {
			controller.getSettingsPathLabel().setText(OutputFiles.deriveSettingsPath(newVal));
			controller.getTreesPathLabel().setText(OutputFiles.deriveTreeInfoPath(newVal));
			controller.getLogPathLabel().setText(OutputFiles.deriveLogPath(newVal));
			controller.getBootstrapPathLabel().setText(OutputFiles.deriveBootstrapPath(newVal));
		});
	}

	// =================================================================
	//  Helpers
	// =================================================================

	private Window ownerWindow() {
		var scene = controller.getAlignmentFileTextField().getScene();
		return scene != null ? scene.getWindow() : null;
	}

	// =================================================================
	//  Output tab — defaults + bindings
	// =================================================================

	private void configureOutputTab() {
		controller.getPiclExecutableTextField().setText(PiclExtractor.resolveExecutable().toFile().getAbsolutePath());

		// Disable Run while running OR while no alignment has been set.
		// Stop is the inverse — only enabled while a process is alive.
		controller.getRunPiclButton().disableProperty().bind(
				running.or(controller.getAlignmentFileTextField().textProperty().isEmpty()));
		controller.getStopRunButton().disableProperty().bind(running.not());

		// Empty Output → Clear/Copy disabled.
		controller.getClearOutputButton().disableProperty().bind(controller.getLogView().emptyProperty());
		controller.getCopyOutputButton().disableProperty().bind(controller.getLogView().emptyProperty());

		// Indeterminate progress bar visible only while a run is in flight.
		var runProgressBar = controller.getRunProgressBar();
		runProgressBar.setProgress(-1.0);
		runProgressBar.visibleProperty().bind(running);
		runProgressBar.managedProperty().bind(running);

		controller.getRunStatusLabel().setText("Idle");

		// Tree-tab buttons disabled until a tree is loaded.
		controller.getSaveTreeAsButton().disableProperty().bind(
				controller.getOutputTextArea().textProperty().isEmpty());
	}

	// =================================================================
	//  ChoiceBox population
	// =================================================================

	private void configureChoiceBoxes() {
		var modelChoiceBox = controller.getModelChoiceBox();
		var branchLengthMethodChoiceBox = controller.getBranchLengthMethodChoiceBox();
		var treeSearchMethodChoiceBox = controller.getTreeSearchMethodChoiceBox();

		modelChoiceBox.setItems(FXCollections.observableArrayList(Settings.Model.values()));

		branchLengthMethodChoiceBox.setItems(
				FXCollections.observableArrayList(Settings.BranchLengthMethod.values()));
		// ChoiceBox doesn't support per-item disabling; if the user picks
		// the not-implemented one we revert to UPHILL.
		branchLengthMethodChoiceBox.valueProperty().addListener((obs, prev, next) -> {
			if (next != null && !next.isImplemented()) {
				branchLengthMethodChoiceBox.setValue(Settings.BranchLengthMethod.UPHILL);
				controller.getStatusLabel().setText(next.displayName() + " is not yet implemented in PICL.");
			}
		});

		treeSearchMethodChoiceBox.setItems(
				FXCollections.observableArrayList(Settings.TreeSearchMethod.values()));
	}

	// =================================================================
	//  Species / lineage table
	// =================================================================

	private void configureTable() {
		var lineageIndexColumn = controller.getLineageIndexColumn();
		var lineageNameColumn = controller.getLineageNameColumn();
		var speciesAssignmentColumn = controller.getSpeciesAssignmentColumn();
		var lineageSpeciesTableView = controller.getLineageSpeciesTableView();

		lineageIndexColumn.setCellValueFactory(c ->
				new SimpleObjectProperty<>(c.getValue().getIndex()));
		lineageNameColumn.setCellValueFactory(c ->
				new SimpleObjectProperty<>(c.getValue().getLineage()));
		speciesAssignmentColumn.setCellValueFactory(c ->
				new SimpleObjectProperty<>(c.getValue().getSpecies()));

		// Species column: dropdown bound to the live species list from the
		// settings file. Falls back to free-text editing if the species list
		// is empty (e.g., before any file has been loaded).
		speciesAssignmentColumn.setCellFactory(col ->
				settings.getSpecies().isEmpty()
						? TextFieldTableCell.<Settings.LineageAssignment>forTableColumn().call(col)
						: ChoiceBoxTableCell.<Settings.LineageAssignment, String>forTableColumn(
						settings.getSpecies()).call(col));
		settings.getSpecies().addListener((javafx.collections.ListChangeListener<String>) c ->
				lineageSpeciesTableView.refresh());
		speciesAssignmentColumn.setOnEditCommit(e -> {
			e.getRowValue().setSpecies(e.getNewValue());
			updateCountLabels();
		});

		lineageSpeciesTableView.setEditable(true);
		lineageSpeciesTableView.setItems(settings.getLineageAssignments());
	}

	// =================================================================
	//  Enable / disable dependencies
	// =================================================================

	private void configureEnableDisableBindings() {
		// Gamma rate / categories live only in the CIS+gamma model.
		var notGamma = controller.getModelChoiceBox().valueProperty()
				.isNotEqualTo(Settings.Model.CIS_GAMMA);
		controller.getGammaRateTextField().disableProperty().bind(notGamma);
		controller.getGammaRateHintLabel().disableProperty().bind(notGamma);
		controller.getGammaCategoriesTextField().disableProperty().bind(notGamma);
		controller.getGammaCategoriesHintLabel().disableProperty().bind(notGamma);

		// Tree-file row is only meaningful when reading from a tree file.
		var notFromFile = controller.getReadFromTreeFileRadioButton().selectedProperty().not();
		controller.getTreeFileTextField().disableProperty().bind(notFromFile);
		controller.getTreeFileBrowseButton().disableProperty().bind(notFromFile);
		controller.getUseBranchLengthsFromTreeCheckBox().disableProperty().bind(notFromFile);

		// Cooling rate β is used only by simulated-annealing NNI.
		var notSA = controller.getTreeSearchMethodChoiceBox().valueProperty()
				.isNotEqualTo(Settings.TreeSearchMethod.SA_NNI);
		controller.getCoolingRateTextField().disableProperty().bind(notSA);
		controller.getCoolingRateHintLabel().disableProperty().bind(notSA);
	}

	// =================================================================
	//  Count labels in the species/lineages section
	// =================================================================

	private void configureCountLabels() {
		controller.getLineagesCountLabel().textProperty().bind(
				Bindings.size(settings.getLineageAssignments()).asString("%d lineages"));
		updateCountLabels();
		settings.getLineageAssignments().addListener(
				(javafx.collections.ListChangeListener<Settings.LineageAssignment>) c -> updateCountLabels());
	}

	private void updateCountLabels() {
		int count = settings.getSpecies().isEmpty()
				? (int) settings.getLineageAssignments().stream()
				.map(Settings.LineageAssignment::getSpecies)
				.filter(s -> s != null && !s.isBlank())
				.distinct().count()
				: settings.getSpecies().size();
		controller.getSpeciesCountLabel().setText(count + " species");
	}

	// =================================================================
	//  Button wiring
	// =================================================================

	private void wireButtonHandlers() {
		controller.getLoadSettingsButton().setOnAction(this::onLoadSettings);

		controller.getAlignmentBrowseButton().setOnAction(e -> browseForFile(
				controller.getAlignmentFileTextField(), "Multiple sequence alignment",
				"*.phy", "*.phylip", "*.fasta", "*.fna"));
		controller.getOutTreeFileBrowseButton().setOnAction(e -> browseForSaveFile(
				controller.getOutTreeFileTextField(), "Newick tree",
				"*.tre", "*.tree", "*.nwk", "*.newick"));
		controller.getTreeFileBrowseButton().setOnAction(e -> browseForFile(
				controller.getTreeFileTextField(), "Tree files",
				"*.tre", "*.tree", "*.nwk", "*.newick", "*.nex", "*.nxs"));

		controller.getRandomiseSeed1Button().setOnAction(e ->
				controller.getRandomSeed1TextField().setText(Long.toString(nextSeed())));
		controller.getRandomiseSeed2Button().setOnAction(e ->
				controller.getRandomSeed2TextField().setText(Long.toString(nextSeed())));

		controller.getCancelButton().setOnAction(this::onCancel);
		controller.getValidateButton().setOnAction(this::onValidate);
		controller.getPreviewSettingsButton().setOnAction(this::onPreview);
		controller.getRunPiclButton().setOnAction(this::onRunPicl);

		controller.getClearSpeciesListButton().setOnAction(e -> controller.getLineageSpeciesTableView().getItems().clear());
		controller.getClearSpeciesListButton().disableProperty().bind(Bindings.isEmpty(controller.getLineageSpeciesTableView().getItems()));

		controller.getLineagesFromDataButton().setOnAction(this::onLineagesFromData);
		controller.getLineagesFromDataButton().disableProperty().bind(Bindings.isNotEmpty(controller.getLineageSpeciesTableView().getItems()));

		controller.getAutoDetectSpeciesByPrefixButton().setOnAction(this::onAutoDetectSpeciesByPrefix);
		controller.getImportSpeciesMappingButton().setOnAction(this::onImportSpeciesMapping);

		controller.getPiclExecutableBrowseButton().setOnAction(e -> browseForFile(
				controller.getPiclExecutableTextField(), "Executable", "*"));
		controller.getClearOutputButton().setOnAction(e -> controller.getLogView().clear());
		controller.getCopyOutputButton().setOnAction(this::onCopyOutput);
		controller.getStopRunButton().setOnAction(this::onStopRun);

		controller.getReloadTreeButton().setOnAction(this::onReloadTree);
		controller.getSaveTreeAsButton().setOnAction(this::onSaveTreeAs);
	}

	// =================================================================
	//  UI ⇄ Settings synchronisation
	// =================================================================

	public void applyToUi(Settings s) {
		controller.getModelChoiceBox().setValue(s.getModel());
		if (false)
			controller.getAlignmentFileTextField().setText(s.getAlignmentFile());
		controller.getIncludeAllSitesCheckBox().setSelected(s.isIncludeAllSites());
		controller.getThetaTextField().setText(Double.toString(s.getTheta()));
		controller.getGammaRateTextField().setText(Double.toString(s.getGammaRate()));
		controller.getGammaCategoriesTextField().setText(Integer.toString(s.getGammaCategories()));

		if (s.getStartingTreeSource() == Settings.StartingTreeSource.READ_FROM_FILE)
			controller.getReadFromTreeFileRadioButton().setSelected(true);
		else
			controller.getGenerateRandomTreeRadioButton().setSelected(true);
		if (false)
			controller.getTreeFileTextField().setText(s.getTreeFile());
		controller.getUseBranchLengthsFromTreeCheckBox().setSelected(s.isUseBranchLengthsFromTree());

		controller.getBranchLengthMethodChoiceBox().setValue(s.getBranchLengthMethod());
		controller.getBranchLengthIterationsTextField().setText(Long.toString(s.getBranchLengthIterations()));

		controller.getTreeSearchMethodChoiceBox().setValue(s.getTreeSearchMethod());
		controller.getTreeSearchIterationsTextField().setText(Long.toString(s.getTreeSearchIterations()));
		controller.getMultiIterTextField().setText(Integer.toString(s.getMultiIter()));
		controller.getProbBoundTextField().setText(Double.toString(s.getProbBound()));
		controller.getTestIncrTextField().setText(Integer.toString(s.getTestIncr()));
		controller.getOptSlopeTextField().setText(Double.toString(s.getOptSlope()));
		controller.getCoolingRateTextField().setText(Double.toString(s.getCoolingRate()));

		controller.getBootstrapReplicatesTextField().setText(Integer.toString(s.getBootstrapReplicates()));
		controller.getVerboseOutputCheckBox().setSelected(s.isVerboseOutput());
		controller.getRandomSeed1TextField().setText(Long.toString(s.getRandomSeed1()));
		controller.getRandomSeed2TextField().setText(Long.toString(s.getRandomSeed2()));

		// Replace the table contents only if the incoming Settings has its own list.
		if (s != this.settings) {
			settings.getLineageAssignments().setAll(s.getLineageAssignments());
		}
		updateCountLabels();
	}

	public void pullFromUi(Settings s) {
		s.setModel(controller.getModelChoiceBox().getValue());
		s.setAlignmentFile(controller.getAlignmentFileTextField().getText());
		s.setIncludeAllSites(controller.getIncludeAllSitesCheckBox().isSelected());
		s.setTheta(parseDouble(controller.getThetaTextField(), s.getTheta()));
		s.setGammaRate(parseDouble(controller.getGammaRateTextField(), s.getGammaRate()));
		s.setGammaCategories(parseInt(controller.getGammaCategoriesTextField(), s.getGammaCategories()));

		s.setStartingTreeSource(controller.getReadFromTreeFileRadioButton().isSelected()
				? Settings.StartingTreeSource.READ_FROM_FILE
				: Settings.StartingTreeSource.GENERATE_RANDOM);
		s.setTreeFile(controller.getTreeFileTextField().getText());
		s.setUseBranchLengthsFromTree(controller.getUseBranchLengthsFromTreeCheckBox().isSelected());

		s.setBranchLengthMethod(controller.getBranchLengthMethodChoiceBox().getValue());
		s.setBranchLengthIterations(parseLong(controller.getBranchLengthIterationsTextField(),
				s.getBranchLengthIterations()));

		s.setTreeSearchMethod(controller.getTreeSearchMethodChoiceBox().getValue());
		s.setTreeSearchIterations(parseLong(controller.getTreeSearchIterationsTextField(),
				s.getTreeSearchIterations()));
		s.setMultiIter(parseInt(controller.getMultiIterTextField(), s.getMultiIter()));
		s.setProbBound(parseDouble(controller.getProbBoundTextField(), s.getProbBound()));
		s.setTestIncr(parseInt(controller.getTestIncrTextField(), s.getTestIncr()));
		s.setOptSlope(parseDouble(controller.getOptSlopeTextField(), s.getOptSlope()));
		s.setCoolingRate(parseDouble(controller.getCoolingRateTextField(), s.getCoolingRate()));

		s.setBootstrapReplicates(parseInt(controller.getBootstrapReplicatesTextField(),
				s.getBootstrapReplicates()));
		s.setVerboseOutput(controller.getVerboseOutputCheckBox().isSelected());
		s.setRandomSeed1(parseLong(controller.getRandomSeed1TextField(), s.getRandomSeed1()));
		s.setRandomSeed2(parseLong(controller.getRandomSeed2TextField(), s.getRandomSeed2()));
		// Lineage assignments are already shared via the ObservableList.
	}

	// =================================================================
	//  Action handlers
	// =================================================================

	private void onLoadSettings(ActionEvent e) {
		var chooser = new FileChooser();
		chooser.setTitle("Load PICL settings");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("PICL settings", "*.txt", "*.cfg", "*.settings", "*"));
		if (lastSettingsFile != null && lastSettingsFile.getParentFile() != null)
			chooser.setInitialDirectory(lastSettingsFile.getParentFile());
		var file = chooser.showOpenDialog(window());
		if (file == null) return;
		try {
			var loaded = Settings.read(file.toPath());
			applyToUi(loaded);
			pullFromUi(this.settings);
			settings.getSpecies().setAll(loaded.getSpecies());
			settings.getLineageAssignments().setAll(loaded.getLineageAssignments());
			lastSettingsFile = file;
			controller.getStatusLabel().setText("Loaded " + file.getName()
												+ " · " + loaded.getSpecies().size() + " species, "
												+ loaded.getLineageAssignments().size() + " lineages");
		} catch (Exception ex) {
			error("Could not load settings", ex);
		}
	}

	private void onSaveSettings(ActionEvent e) {
		var chooser = new FileChooser();
		chooser.setTitle("Save PICL settings");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("PICL settings", "*.txt", "*.cfg", "*.settings"));
		if (lastSettingsFile != null) {
			chooser.setInitialDirectory(lastSettingsFile.getParentFile());
			chooser.setInitialFileName(lastSettingsFile.getName());
		} else {
			chooser.setInitialFileName("picl.settings");
		}
		var file = chooser.showSaveDialog(window());
		if (file == null) return;
		try {
			pullFromUi(settings);
			settings.write(file.toPath());
			lastSettingsFile = file;
			controller.getStatusLabel().setText("Saved " + file.getName());
		} catch (Exception ex) {
			error("Could not save settings", ex);
		}
	}

	private void browseForFile(TextField target, String description, String... patterns) {
		var chooser = new FileChooser();
		chooser.setTitle("Choose file");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, patterns));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
		var current = target.getText();
		if (current != null && !current.isBlank()) {
			var f = new File(current);
			if (f.getParentFile() != null && f.getParentFile().isDirectory())
				chooser.setInitialDirectory(f.getParentFile());
			chooser.setInitialFileName(f.getName());
		}
		var file = chooser.showOpenDialog(window());
		if (file != null) target.setText(file.getAbsolutePath());
	}

	private void browseForSaveFile(TextField target, String description, String... patterns) {
		var chooser = new FileChooser();
		chooser.setTitle("Choose file");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, patterns));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
		var current = target.getText();
		if (current != null && !current.isBlank()) {
			var f = new File(current);
			if (f.getParentFile() != null && f.getParentFile().isDirectory())
				chooser.setInitialDirectory(f.getParentFile());
			chooser.setInitialFileName(f.getName());
		}
		var file = chooser.showSaveDialog(window());
		if (file != null) target.setText(file.getAbsolutePath());
	}

	private void onCancel(ActionEvent e) {
		// Cancel halts a running picl process. It does NOT close the
		// window — the user can still review the log, edit settings,
		// or relaunch. If picl isn't running, this is a no-op.
		if (currentProcess == null || !currentProcess.isAlive()) {
			controller.getStatusLabel().setText("Nothing to cancel — picl isn't running.");
			return;
		}
		onStopRun(e);
	}

	private void onValidate(ActionEvent e) {
		pullFromUi(settings);
		var problems = settings.validate();
		if (problems.isEmpty()) {
			controller.getStatusLabel().setText(
					"Settings OK · " + settings.getLineageAssignments().size() + " lineages");
			new Alert(Alert.AlertType.INFORMATION, "Settings look good.", ButtonType.OK).showAndWait();
		} else {
			controller.getStatusLabel().setText(problems.size() + " problem(s) found");
			var msg = String.join("\n• ", problems);
			new Alert(Alert.AlertType.WARNING, "• " + msg, ButtonType.OK).showAndWait();
		}
	}

	private void onPreview(ActionEvent e) {
		pullFromUi(settings);
		var area = new TextArea(settings.preview());
		area.setEditable(false);
		area.setPrefRowCount(24);
		area.setPrefColumnCount(60);
		area.setStyle("-fx-font-family: 'monospace';");
		var dialog = new Alert(Alert.AlertType.NONE, null, ButtonType.CLOSE);
		dialog.setTitle("Settings preview");
		dialog.setHeaderText("Settings file as it will be written");
		dialog.getDialogPane().setContent(new VBox(area));
		dialog.showAndWait();
	}

	private void onRunPicl(ActionEvent e) {
		pullFromUi(settings);

		var problems = settings.validate();
		if (!problems.isEmpty()) {
			controller.getStatusLabel().setText("Cannot run — " + problems.size() + " problem(s)");
			new Alert(Alert.AlertType.WARNING,
					"Fix these first:\n• " + String.join("\n• ", problems),
					ButtonType.OK).showAndWait();
			return;
		}

		// Resolve the PICL executable.
		var execPath = Paths.get(controller.getPiclExecutableTextField().getText().trim());
		if (!Files.isExecutable(execPath)) {
			new Alert(Alert.AlertType.ERROR,
					"PICL executable not found or not executable:\n" + execPath,
					ButtonType.OK).showAndWait();
			return;
		}

		// Resolve the alignment file. PICL will fopen() it directly.
		var alignmentText = controller.getAlignmentFileTextField().getText().trim();
		if (alignmentText.isBlank()) {
			new Alert(Alert.AlertType.ERROR,
					"Please choose an alignment file first.",
					ButtonType.OK).showAndWait();
			return;
		}
		var alignmentPath = Paths.get(alignmentText).toAbsolutePath();
		if (!Files.isReadable(alignmentPath)) {
			new Alert(Alert.AlertType.ERROR,
					"Alignment file not found or not readable:\n" + alignmentPath,
					ButtonType.OK).showAndWait();
			return;
		}

		// Resolve the picltrees.tre path (the user-controlled "Output tree").
		// Default to a sibling of the alignment if blank.
		var outTreeText = controller.getOutTreeFileTextField().getText().trim();
		if (outTreeText.isBlank())
			outTreeText = OutputFiles.deriveOutputTreePath(alignmentPath.toString());

		// Collision handling: first check the natural (suffix == 1)
		// names. If nothing collides we proceed silently. Otherwise we
		// surface a Finder-style "Keep Both / Stop / Replace" prompt:
		//   - Keep Both → bump the whole set to the smallest free -N
		//   - Replace   → delete the colliding natural-name files
		//   - Stop      → cancel the run
		// PICL appends to some files, so writing into existing ones
		// would silently mix runs — every branch keeps a run's output
		// coherent.
		var natural = OutputFiles.naturalPaths(alignmentPath.toString(), outTreeText);
		OutputFiles.BumpedPaths bumped;
		if (natural.allClear()) {
			bumped = natural;
		} else {
			var choice = OutputFiles.promptOnCollision(natural, ownerWindow());
			switch (choice) {
				case STOP -> {
					controller.getStatusLabel().setText("Run cancelled.");
					return;
				}
				case KEEP_BOTH -> {
					bumped = OutputFiles.bumpUntilFree(alignmentPath.toString(), outTreeText);
					// Reflect the chosen name in the UI so the user sees
					// what PICL will actually write (and so the path
					// labels for .trees / .log / .bootstrap auto-update
					// via their existing text-property listeners).
					controller.getOutTreeFileTextField().setText(bumped.outTree().toString());
					controller.getStatusLabel().setText("Output exists; writing to " + bumped.outTree().getFileName());
				}
				case REPLACE -> {
					try {
						OutputFiles.deleteSet(natural);
					} catch (IOException ex) {
						error("Could not delete existing output files", ex);
						return;
					}
					bumped = natural;
					controller.getStatusLabel().setText(
							"Replaced existing " + bumped.outTree().getFileName());
				}
				default -> {
					return;
				}
			}
		}

		var picltreesPath = bumped.outTree();
		var settingsPath = bumped.settings();    // user-visible (always original names)
		var treesPath = bumped.treesInfo();
		var valuesPath = bumped.values();
		var logPath = bumped.log();
		var bootstrapPath = bumped.bootstrap();

		// Format detection. PICL accepts relaxed Phylip with long names,
		// so FASTA inputs just get re-rendered to Phylip (in the system
		// temp dir) without any name remapping. Phylip inputs pass through.
		Path piclAlignmentPath;
		try {
			piclAlignmentPath = prepareAlignmentForPicl(alignmentPath);
		} catch (IOException ex) {
			error("Could not read alignment", ex);
			return;
		}

		// Starting tree file path — only consulted by PICL when
		// Random_tree=0. Use the user's value if set, else a sibling
		// of the alignment as a fallback.
		var treeFileText = controller.getTreeFileTextField().getText().trim();
		Path treeFilePath;
		if (treeFileText.isBlank()) {
			treeFilePath = alignmentPath.resolveSibling("treefile.tre");
		} else {
			var tfp = Paths.get(treeFileText);
			treeFilePath = tfp.isAbsolute()
					? tfp
					: alignmentPath.resolveSibling(tfp).toAbsolutePath();
		}

		// Write the settings file once — it's both the user-visible
		// output artifact and PICL's argv[1], using identical contents.
		try {
			settings.write(settingsPath);
		} catch (IOException ex) {
			error("Could not write settings file", ex);
			return;
		}

		// Run picl with cwd = the alignment's directory.
		var workDir = alignmentPath.getParent() != null
				? alignmentPath.getParent().toFile()
				: new File(System.getProperty("user.dir"));

		// Switch the user to the Log tab and clear previous output.
		controller.getMainTabPane().getSelectionModel().select(controller.getLogTab());
		controller.getLogView().clear();

		// Open the log file before any appendOutput call.
		try {
			logFileWriter = Files.newBufferedWriter(logPath);
			appendOutput(Version.SHORT_DESCRIPTION + "\n");
		} catch (IOException ex) {
			logFileWriter = null;
			controller.getStatusLabel().setText("Could not open log file: " + ex.getMessage());
		}

		if (settings.isVerboseOutput()) {
			appendOutput("$ " + execPath
						 + " " + settingsPath
						 + " " + piclAlignmentPath
						 + " " + treeFilePath
						 + " " + treesPath
						 + " " + picltreesPath
						 + " " + valuesPath
						 + " " + bootstrapPath + "\n");
			if (!piclAlignmentPath.equals(alignmentPath)) {
				appendOutput("(FASTA input detected; converted to Phylip at " + piclAlignmentPath + ")\n");
			}
		}

		// argv layout:
		//   argv[1] = settings
		//   argv[2] = data.phy (alignment — the converted temp Phylip if input was FASTA)
		//   argv[3] = treefile.tre (input starting tree, if Random_tree=0)
		//   argv[4] = outtree.tre (renamed to <output>.trees)
		//   argv[5] = picltrees.tre (the user's Output tree — Newick only)
		//   argv[6] = values
		//   argv[7] = bootstrap
		var pb = new ProcessBuilder(execPath.toString(), settingsPath.toString(), piclAlignmentPath.toString(),
				treeFilePath.toString(), treesPath.toString(), picltreesPath.toString(), valuesPath.toString(), bootstrapPath.toString())
				.directory(workDir)
				.redirectErrorStream(true);

		this.pendingTreeInfoPath = treesPath;
		this.pendingOutTreePath = picltreesPath;
		try {
			currentProcess = pb.start();
		} catch (IOException ex) {
			error("Could not launch PICL", ex);
			return;
		}

		running.set(true);
		controller.getRunStatusLabel().setText("Running…");
		controller.getStatusLabel().setText("PICL is running");

		var process = currentProcess;
		var reader = new Thread(() -> streamProcessOutput(process), "picl-output-reader");
		reader.setDaemon(true);
		reader.start();
	}

	private void streamProcessOutput(Process process) {
		try (var in = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = in.readLine()) != null) {
				final String captured = line;
				Platform.runLater(() -> appendOutput(captured + "\n"));
			}
		} catch (IOException ex) {
			Platform.runLater(() -> appendOutput("[stream error] " + ex.getMessage() + "\n"));
		}

		int exitCode;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			exitCode = -1;
		}
		final int code = exitCode;
		Platform.runLater(() -> onProcessExited(process, code));
	}

	private void onProcessExited(Process process, int exitCode) {
		running.set(false);
		if (currentProcess == process) currentProcess = null;
		appendOutput("\n[picl exited with code " + exitCode + "]\n");

		if (logFileWriter != null) {
			try {
				logFileWriter.close();
			} catch (IOException ignored) {
			}
			logFileWriter = null;
		}

		if (exitCode == 0) {
			controller.getRunStatusLabel().setText("Finished");
			controller.getStatusLabel().setText("PICL finished successfully");

			if (pendingTreeInfoPath != null
				&& loadTreeFromFile(pendingTreeInfoPath)) {
				controller.getMainTabPane().getSelectionModel().select(controller.getOutputTab());
			}

			if (pendingOutTreePath != null) {
				drawTreeFromFile(pendingOutTreePath);
			}
		} else {
			controller.getRunStatusLabel().setText("Failed (exit " + exitCode + ")");
			controller.getStatusLabel().setText("PICL exited with code " + exitCode);
		}
	}

	// =================================================================
	//  Alignment preparation for PICL
	// =================================================================

	/**
	 * Decides what file PICL should actually read as its alignment.
	 * For Phylip input, returns the alignment unchanged. For FASTA,
	 * parses the file and re-renders it as relaxed Phylip in the
	 * system temp dir (with the same taxon names — PICL accepts long
	 * names now, so no remapping is needed).
	 */
	private Path prepareAlignmentForPicl(Path alignmentPath) throws IOException {
		var format = AlignmentFormat.detect(alignmentPath);
		switch (format) {
			case PHYLIP:
				return alignmentPath;
			case FASTA:
				var sequences = FastaParser.parseAligned(alignmentPath);
				var tempPath = Files.createTempFile("picl-input-", ".phy");
				tempPath.toFile().deleteOnExit();
				PhylipWriter.write(tempPath, sequences);
				return tempPath;
			case UNKNOWN:
			default:
				throw new IOException("Unrecognised alignment format (not Phylip or FASTA): "
									  + alignmentPath);
		}
	}

	/**
	 * Reads the given tree-info file into the Output tab's TextArea.
	 * Returns true on success.
	 */
	private boolean loadTreeFromFile(Path treeFile) {
		var pathLabel = controller.getTreesFilePathLabel();
		if (!Files.isReadable(treeFile)) {
			pathLabel.setText(treeFile + "  (not found)");
			return false;
		}
		try {
			var content = Files.readString(treeFile);
			controller.getOutputTextArea().setText(content);
			pathLabel.setText(treeFile.toString());
			lastTreeFile = treeFile;
			return true;
		} catch (IOException ex) {
			pathLabel.setText(treeFile + "  (error)");
			controller.getStatusLabel().setText(
					"Could not read " + treeFile.getFileName() + ": " + ex.getMessage());
			return false;
		}
	}

	// =================================================================
	//  Tree tab — graphical phylogram view of the .tre file
	// =================================================================

	private void configureTreeTab() {
		showTreePlaceholder("Run PICL to see the tree.");
		var pane = controller.getTreeCanvasPane();
		pane.widthProperty().addListener((obs, o, n) -> redrawTree());
		pane.heightProperty().addListener((obs, o, n) -> redrawTree());
	}

	private void showTreePlaceholder(String text) {
		var label = new Label(text);
		label.setLayoutX(20);
		label.setLayoutY(20);
		controller.getTreeCanvasPane().getChildren().setAll(label);
		lastDrawnTreeRoot = null;
	}

	private void redrawTree() {
		if (lastDrawnTreeRoot == null) return;
		var pane = controller.getTreeCanvasPane();
		var w = pane.getWidth();
		var h = pane.getHeight();
		if (w <= 40 || h <= 40) return;
		var pad = 20.0;
		var group = DrawPhylogram.draw(lastDrawnTreeRoot, w - 2 * pad, h - 2 * pad);
		group.setLayoutX(pad);
		group.setLayoutY(pad);
		pane.getChildren().setAll(group);
	}

	private boolean drawTreeFromFile(Path treeFile) {
		var pathLabel = controller.getTreeFilePathLabel();
		if (!Files.isReadable(treeFile)) {
			pathLabel.setText(treeFile + "  (not found)");
			showTreePlaceholder("Tree file not found.");
			return false;
		}
		try {
			var content = Files.readString(treeFile).trim();
			if (content.isEmpty()) {
				pathLabel.setText(treeFile + "  (empty)");
				showTreePlaceholder("Tree file is empty.");
				return false;
			}
			var semi = content.indexOf(';');
			var newick = (semi >= 0) ? content.substring(0, semi + 1) : content;
			lastDrawnTreeRoot = new NewickParser(newick).parse();
			pathLabel.setText(treeFile.toString());
			redrawTree();
			return true;
		} catch (Exception ex) {
			pathLabel.setText(treeFile + "  (error)");
			controller.getStatusLabel().setText(
					"Could not draw " + treeFile.getFileName() + ": " + ex.getMessage());
			showTreePlaceholder("Could not parse tree: " + ex.getMessage());
			return false;
		}
	}

	public String getTreeFromFile(Path treeFile) {
		try {
			var content = Files.readString(treeFile).trim();
			var semi = content.indexOf(';');
			return (semi >= 0) ? content.substring(0, semi + 1) : content;
		} catch (IOException ignored) {
			return "";
		}

	}


	private void onStopRun(ActionEvent e) {
		var p = currentProcess;
		if (p == null || !p.isAlive()) return;
		appendOutput("\n[stopping picl…]\n");
		p.destroy();
		new Thread(() -> {
			try {
				p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
			}
			if (p.isAlive()) p.destroyForcibly();
		}, "picl-stop").start();
	}

	private void onCopyOutput(ActionEvent e) {
		if (!controller.getLogView().copySelectedLines())
			controller.getLogView().copyAllLines();
		controller.getStatusLabel().setText("Output copied to clipboard");
	}

	private void onReloadTree(ActionEvent e) {
		Path target;
		if (lastTreeFile != null) {
			target = lastTreeFile;
		} else {
			var fieldText = controller.getOutTreeFileTextField().getText().trim();
			if (fieldText.isBlank()) {
				controller.getStatusLabel().setText("No tree to reload — run PICL first.");
				return;
			}
			target = Paths.get(fieldText);
		}
		if (loadTreeFromFile(target)) {
			controller.getStatusLabel().setText("Reloaded " + target.getFileName());
		}
	}

	private void onCopyTree(ActionEvent e) {
		var newick = getTreeFromFile(pendingOutTreePath);
		if (!newick.isBlank()) {
			var content = new ClipboardContent();
			content.putString(newick);
			Clipboard.getSystemClipboard().setContent(content);
			controller.getStatusLabel().setText("Tree copied to clipboard");
		}
	}

	private void onSaveTreeAs(ActionEvent e) {
		var chooser = new FileChooser();
		chooser.setTitle("Save trees as");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Tree info", "*.trees"));
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Newick tree", "*.tre", "*.tree", "*.nwk", "*.newick"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
		if (lastTreeFile != null && lastTreeFile.getParent() != null) {
			chooser.setInitialDirectory(lastTreeFile.getParent().toFile());
			chooser.setInitialFileName(lastTreeFile.toFile().getName());
		} else {
			chooser.setInitialFileName("output.trees");
		}
		var file = chooser.showSaveDialog(window());
		if (file == null) return;
		try {
			Files.writeString(file.toPath(), controller.getOutputTextArea().getText());
			controller.getStatusLabel().setText("Saved to " + file.getName());
		} catch (IOException ex) {
			error("Could not save", ex);
		}
	}

	/**
	 * Append text on the FX thread to both the Log tab and the on-disk
	 * .log file (when one is open).
	 */
	private void appendOutput(String text) {
		controller.getLogView().appendText(text);
		if (logFileWriter != null) {
			try {
				logFileWriter.write(text);
				logFileWriter.flush();
			} catch (IOException ex) {
				try {
					logFileWriter.close();
				} catch (IOException ignored) {
				}
				logFileWriter = null;
				controller.getStatusLabel().setText("Log file write failed: " + ex.getMessage());
			}
		}
	}

	private void onLineagesFromData(ActionEvent e) {
		if (!controller.getAlignmentFileTextField().getText().isBlank()) {
			var file = new File(controller.getAlignmentFileTextField().getText());
			if (file.exists() && file.canRead()) {
				try {
					var sequences = Alignment.parse(file.toPath());
					controller.getLineageSpeciesTableView().getItems().clear();
					sequences.stream().map(Alignment.Sequence::name)
							.forEach(name -> {
								var id = controller.getLineageSpeciesTableView().getItems().size() + 1;
								var species = (char) ('a' + id - 1);
								controller.getLineageSpeciesTableView().getItems()
										.add(new Settings.LineageAssignment(controller.getLineageSpeciesTableView().getItems().size() + 1, name, "" + species));
							});
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

	}

	private void onAutoDetectSpeciesByPrefix(ActionEvent e) {
		var seen = new java.util.LinkedHashSet<String>();
		for (var la : settings.getLineageAssignments()) {
			var name = la.getLineage();
			if (name == null) continue;
			int dot = name.indexOf('.');
			int us = name.indexOf('_');
			int cut = (dot >= 0 && (us < 0 || dot < us)) ? dot : us;
			if (cut > 0) {
				var sp = name.substring(0, cut);
				la.setSpecies(sp);
				seen.add(sp);
			}
		}
		settings.getSpecies().setAll(seen);
		controller.getLineageSpeciesTableView().refresh();
		updateCountLabels();
	}

	private void onImportSpeciesMapping(ActionEvent e) {
		var chooser = new FileChooser();
		chooser.setTitle("Import species mapping (lineage<TAB>species per line)");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tab-separated", "*.tsv", "*.txt"));
		var file = chooser.showOpenDialog(window());
		if (file == null) return;
		try {
			var rows = settings.getLineageAssignments();
			var byName = new java.util.HashMap<String, Settings.LineageAssignment>();
			for (var la : rows) byName.put(la.getLineage(), la);
			for (var line : Files.readAllLines(file.toPath())) {
				if (line.isBlank() || line.startsWith("#")) continue;
				var parts = line.split("\t", 2);
				if (parts.length < 2) continue;
				var la = byName.get(parts[0]);
				if (la != null) la.setSpecies(parts[1]);
			}
			controller.getLineageSpeciesTableView().refresh();
			updateCountLabels();
			controller.getStatusLabel().setText("Mapping imported from " + file.getName());
		} catch (IOException ex) {
			error("Could not import mapping", ex);
		}
	}

	// =================================================================
	//  Small helpers
	// =================================================================

	private long nextSeed() {
		return random.nextInt(Integer.MAX_VALUE);
	}

	private Window window() {
		var cancelButton = controller.getCancelButton();
		return cancelButton == null ? null : cancelButton.getScene().getWindow();
	}

	private static double parseDouble(TextField tf, double dflt) {
		try {
			return Double.parseDouble(tf.getText().trim());
		} catch (Exception ex) {
			return dflt;
		}
	}

	private static int parseInt(TextField tf, int dflt) {
		try {
			return Integer.parseInt(tf.getText().trim());
		} catch (Exception ex) {
			return dflt;
		}
	}

	private static long parseLong(TextField tf, long dflt) {
		try {
			return Long.parseLong(tf.getText().trim());
		} catch (Exception ex) {
			return dflt;
		}
	}

	private void error(String header, Throwable t) {
		controller.getStatusLabel().setText(header + ": " + t.getMessage());
		var a = new Alert(Alert.AlertType.ERROR,
				t.getMessage() == null ? t.toString() : t.getMessage(),
				ButtonType.OK);
		a.setHeaderText(header);
		a.showAndWait();
	}
}
