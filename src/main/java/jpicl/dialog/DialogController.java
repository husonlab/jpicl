package jpicl.dialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Controller for the PICL settings dialog (Dialog.fxml / PiclSettingsView.fxml).
 * <p>
 * The {@link Settings} instance is the source of truth. The UI is populated
 * from it ({@link #applyToUi(Settings)}), and edits are pushed back into it
 * ({@link #pullFromUi(Settings)}) on demand.
 */
public class DialogController {

	// -----------------------------------------------------------------
	//  @FXML fields  (typed generics added for the choice boxes / table)
	// -----------------------------------------------------------------

	@FXML
	private Button alignmentBrowseButton;
	@FXML
	private TextField alignmentFileTextField;
	@FXML
	private Button autoDetectSpeciesByPrefixButton;
	@FXML
	private TextField bootstrapReplicatesTextField;
	@FXML
	private TextField branchLengthIterationsTextField;
	@FXML
	private ChoiceBox<Settings.BranchLengthMethod> branchLengthMethodChoiceBox;
	@FXML
	private Button cancelButton;
	@FXML
	private Label coolingRateHintLabel;
	@FXML
	private TextField coolingRateTextField;
	@FXML
	private Label gammaCategoriesHintLabel;
	@FXML
	private TextField gammaCategoriesTextField;
	@FXML
	private Label gammaRateHintLabel;
	@FXML
	private TextField gammaRateTextField;
	@FXML
	private RadioButton generateRandomTreeRadioButton;
	@FXML
	private Button importSpeciesMappingButton;
	@FXML
	private CheckBox includeAllSitesCheckBox;
	@FXML
	private TableColumn<Settings.LineageAssignment, Integer> lineageIndexColumn;
	@FXML
	private TableColumn<Settings.LineageAssignment, String> lineageNameColumn;
	@FXML
	private TableView<Settings.LineageAssignment> lineageSpeciesTableView;
	@FXML
	private Label lineagesCountLabel;
	@FXML
	private Button loadSettingsButton;
	@FXML
	private ChoiceBox<Settings.Model> modelChoiceBox;
	@FXML
	private Button previewSettingsButton;
	@FXML
	private TextField randomSeed1TextField;
	@FXML
	private TextField randomSeed2TextField;
	@FXML
	private Button randomiseSeed1Button;
	@FXML
	private Button randomiseSeed2Button;
	@FXML
	private RadioButton readFromTreeFileRadioButton;
	@FXML
	private Button runPiclButton;
	@FXML
	private Button saveSettingsButton;
	@FXML
	private TableColumn<Settings.LineageAssignment, String> speciesAssignmentColumn;
	@FXML
	private Label speciesCountLabel;
	@FXML
	private Label statusLabel;
	@FXML
	private TextField thetaTextField;
	@FXML
	private Button treeFileBrowseButton;
	@FXML
	private TextField treeFileTextField;
	@FXML
	private TextField treeSearchIterationsTextField;
	@FXML
	private ChoiceBox<Settings.TreeSearchMethod> treeSearchMethodChoiceBox;
	@FXML
	private CheckBox useBranchLengthsFromTreeCheckBox;
	@FXML
	private Button validateButton;
	@FXML
	private CheckBox verboseOutputCheckBox;

	// Files section at top of Settings tab
	@FXML
	private TextField outTreeFileTextField;
	@FXML
	private Button outTreeFileBrowseButton;
	@FXML
	private Label settingsPathLabel;
	@FXML
	private Label treeInfoPathLabel;
	@FXML
	private Label resultsPathLabel;

	// Output tab
	@FXML
	private TabPane mainTabPane;
	@FXML
	private Tab settingsTab;
	@FXML
	private Tab outputTab;
	@FXML
	private TextField piclExecutableTextField;
	@FXML
	private Button piclExecutableBrowseButton;
	@FXML
	private Label runStatusLabel;
	@FXML
	private ProgressBar runProgressBar;
	@FXML
	private Button clearOutputButton;
	@FXML
	private Button copyOutputButton;
	@FXML
	private Button stopRunButton;
	@FXML
	private TextArea outputTextArea;

	// Tree tab
	@FXML
	private Tab treeTab;
	@FXML
	private Label treeFilePathLabel;
	@FXML
	private Button reloadTreeButton;
	@FXML
	private Button copyTreeButton;
	@FXML
	private Button saveTreeAsButton;
	@FXML
	private TextArea treeTextArea;

	// -----------------------------------------------------------------
	//  Internal state
	// -----------------------------------------------------------------

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
	 * Default location of the PICL binary, relative to the JVM's working dir.
	 */
	private static final Path DEFAULT_PICL_EXECUTABLE =
			Paths.get(System.getProperty("user.dir"), "native", "picl", "src", "picl");

	/**
	 * Default extension for the output tree (replaces the alignment extension).
	 */
	private static final String TREE_EXTENSION = ".tre";

	/**
	 * Default extension for the settings file (replaces the output tree extension).
	 */
	private static final String SETTINGS_EXTENSION = ".settings";

	/**
	 * Default extension for the tree-info file (renamed PICL "outtree.tre").
	 */
	private static final String TREEINFO_EXTENSION = ".treeinfo";

	/**
	 * Default extension for the results file (uses the alignment basename).
	 */
	private static final String RESULTS_EXTENSION = ".results";

	/**
	 * Last tree file path loaded into the Tree tab (null if none).
	 */
	private Path lastTreeFile;

	/**
	 * The output-tree path we last derived automatically from the alignment.
	 * If the user-visible value still equals this, alignment changes will
	 * re-derive it; if the user has typed something else, we leave it alone.
	 */
	private String lastAutoDerivedOutputTree = "";

	/**
	 * Output-tree path passed to picl in the most recent run; null if no run yet.
	 */
	private Path pendingOutTreePath;

	public Settings getSettings() {
		return settings;
	}

	// =================================================================
	//  Initialisation — called by FXMLLoader after fields are injected
	// =================================================================

	@FXML
	public void initialize() {
		configureChoiceBoxes();
		configureTable();
		configureEnableDisableBindings();
		configureCountLabels();
		configureFilesSection();
		configureOutputTab();
		wireButtonHandlers();

		applyToUi(settings);   // populate with defaults
		statusLabel.setText("Ready");
	}

	/**
	 * Wires the alignment ⇒ output-tree ⇒ settings-path derivation chain
	 * at the top of the Settings tab.
	 */
	private void configureFilesSection() {
		// When the alignment changes, re-derive the output tree path —
		// but only if the user hasn't customized it (i.e. it still matches
		// whatever we last auto-derived). Also refresh the Results label.
		alignmentFileTextField.textProperty().addListener((obs, oldVal, newVal) -> {
			var derived = deriveOutputTreePath(newVal);
			var current = outTreeFileTextField.getText();
			if (current == null || current.isBlank()
				|| current.equals(lastAutoDerivedOutputTree)) {
				outTreeFileTextField.setText(derived);
			}
			lastAutoDerivedOutputTree = derived;
			resultsPathLabel.setText(deriveResultsPath(newVal));
		});

		// Settings + treeinfo labels always track the output-tree path live.
		outTreeFileTextField.textProperty().addListener((obs, oldVal, newVal) -> {
			settingsPathLabel.setText(deriveSettingsPath(newVal));
			treeInfoPathLabel.setText(deriveTreeInfoPath(newVal));
		});
	}

	/**
	 * alignment "/path/foo.phy" → "/path/foo.tre". Empty input → empty result.
	 */
	private static String deriveOutputTreePath(String alignmentPath) {
		if (alignmentPath == null || alignmentPath.isBlank()) return "";
		var p = Paths.get(alignmentPath);
		return p.resolveSibling(stripExtension(p.getFileName().toString()) + TREE_EXTENSION)
				.toString();
	}

	/**
	 * outputTree "/path/foo.tre" → "/path/foo.settings". Empty input → "(none)".
	 */
	private static String deriveSettingsPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, SETTINGS_EXTENSION);
	}

	/**
	 * outputTree "/path/foo.tre" → "/path/foo.treeinfo". Empty input → "(none)".
	 */
	private static String deriveTreeInfoPath(String outputTreePath) {
		return deriveSiblingPath(outputTreePath, TREEINFO_EXTENSION);
	}

	/**
	 * alignment "/path/foo.phy" → "/path/foo.results". Empty input → "(none)".
	 */
	private static String deriveResultsPath(String alignmentPath) {
		return deriveSiblingPath(alignmentPath, RESULTS_EXTENSION);
	}

	/**
	 * Replaces the extension on a path, or returns "(none)" for blank input.
	 */
	private static String deriveSiblingPath(String filePath, String newExtension) {
		if (filePath == null || filePath.isBlank()) return "(none)";
		var p = Paths.get(filePath);
		return p.resolveSibling(stripExtension(p.getFileName().toString()) + newExtension)
				.toString();
	}

	/**
	 * "foo.phy" → "foo"; "foo" → "foo"; ".bashrc" → ".bashrc".
	 */
	private static String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return (dot <= 0) ? filename : filename.substring(0, dot);
	}

	/**
	 * Sets defaults and bindings for controls in the Output tab.
	 */
	private void configureOutputTab() {
		piclExecutableTextField.setText(DEFAULT_PICL_EXECUTABLE.toString());

		// Disable Run while running OR while no alignment has been set.
		// Stop is the inverse — only enabled while a process is alive.
		runPiclButton.disableProperty().bind(
				running.or(alignmentFileTextField.textProperty().isEmpty()));
		stopRunButton.disableProperty().bind(running.not());

		// Empty Output → Clear/Copy disabled.
		clearOutputButton.disableProperty().bind(outputTextArea.textProperty().isEmpty());
		copyOutputButton.disableProperty().bind(outputTextArea.textProperty().isEmpty());

		// Indeterminate progress bar visible only while a run is in flight.
		runProgressBar.setProgress(-1.0);                            // indeterminate ("barber pole")
		runProgressBar.visibleProperty().bind(running);
		runProgressBar.managedProperty().bind(running);              // collapse layout when hidden

		runStatusLabel.setText("Idle");

		// Tree-tab buttons disabled until a tree is loaded.
		copyTreeButton.disableProperty().bind(treeTextArea.textProperty().isEmpty());
		saveTreeAsButton.disableProperty().bind(treeTextArea.textProperty().isEmpty());
	}

	// -----------------------------------------------------------------
	//  ChoiceBox population
	// -----------------------------------------------------------------

	private void configureChoiceBoxes() {
		modelChoiceBox.setItems(FXCollections.observableArrayList(Settings.Model.values()));

		// Show only implemented branch-length methods. The hint label below
		// already explains that numerical derivatives are unavailable.
		modelChoiceBox.setTooltip(new Tooltip("Substitution / coalescent model"));

		var branchLengthMethods = FXCollections.observableArrayList(Settings.BranchLengthMethod.values());
		branchLengthMethodChoiceBox.setItems(branchLengthMethods);
		// ChoiceBox doesn't support per-item disabling out of the box; if the
		// user picks the not-implemented one we revert to UPHILL.
		branchLengthMethodChoiceBox.valueProperty().addListener((obs, prev, next) -> {
			if (next != null && !next.isImplemented()) {
				branchLengthMethodChoiceBox.setValue(Settings.BranchLengthMethod.UPHILL);
				statusLabel.setText(next.displayName() + " is not implemented in PICL.");
			}
		});

		treeSearchMethodChoiceBox.setItems(
				FXCollections.observableArrayList(Settings.TreeSearchMethod.values()));
	}

	// -----------------------------------------------------------------
	//  Species / lineage table
	// -----------------------------------------------------------------

	private void configureTable() {
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
		// Refresh cell factory when the species list transitions empty ↔ non-empty.
		settings.getSpecies().addListener((javafx.collections.ListChangeListener<String>) c ->
				lineageSpeciesTableView.refresh());
		speciesAssignmentColumn.setOnEditCommit(e -> {
			e.getRowValue().setSpecies(e.getNewValue());
			updateCountLabels();
		});

		lineageSpeciesTableView.setEditable(true);
		lineageSpeciesTableView.setItems(settings.getLineageAssignments());
	}

	// -----------------------------------------------------------------
	//  Enable / disable dependencies
	// -----------------------------------------------------------------

	private void configureEnableDisableBindings() {
		// Gamma rate / categories live only in the CIS+gamma model.
		var notGamma = modelChoiceBox.valueProperty()
				.isNotEqualTo(Settings.Model.CIS_GAMMA);
		gammaRateTextField.disableProperty().bind(notGamma);
		gammaRateHintLabel.disableProperty().bind(notGamma);
		gammaCategoriesTextField.disableProperty().bind(notGamma);
		gammaCategoriesHintLabel.disableProperty().bind(notGamma);

		// Tree-file row is only meaningful when reading from a tree file.
		var notFromFile = readFromTreeFileRadioButton.selectedProperty().not();
		treeFileTextField.disableProperty().bind(notFromFile);
		treeFileBrowseButton.disableProperty().bind(notFromFile);
		useBranchLengthsFromTreeCheckBox.disableProperty().bind(notFromFile);

		// Cooling rate β is used only by simulated-annealing NNI.
		var notSA = treeSearchMethodChoiceBox.valueProperty()
				.isNotEqualTo(Settings.TreeSearchMethod.SA_NNI);
		coolingRateTextField.disableProperty().bind(notSA);
		coolingRateHintLabel.disableProperty().bind(notSA);
	}

	// -----------------------------------------------------------------
	//  Count labels in the species/lineages section
	// -----------------------------------------------------------------

	private void configureCountLabels() {
		// Lineages count = number of rows in the table.
		lineagesCountLabel.textProperty().bind(
				Bindings.size(settings.getLineageAssignments()).asString("%d lineages"));
		updateCountLabels();
		// Species count: distinct non-blank species names. Recompute on changes.
		settings.getLineageAssignments().addListener(
				(javafx.collections.ListChangeListener<Settings.LineageAssignment>) c -> updateCountLabels());
	}

	private void updateCountLabels() {
		// Prefer the explicit species list (from the settings file); fall back
		// to the distinct non-blank species in the assignments.
		int count = settings.getSpecies().isEmpty()
				? (int) settings.getLineageAssignments().stream()
				.map(Settings.LineageAssignment::getSpecies)
				.filter(s -> s != null && !s.isBlank())
				.distinct().count()
				: settings.getSpecies().size();
		speciesCountLabel.setText(count + " species");
	}

	// -----------------------------------------------------------------
	//  Button wiring
	// -----------------------------------------------------------------

	private void wireButtonHandlers() {
		loadSettingsButton.setOnAction(this::onLoadSettings);
		saveSettingsButton.setOnAction(this::onSaveSettings);

		alignmentBrowseButton.setOnAction(e -> browseForFile(
				alignmentFileTextField, "Phylip alignment",
				"*.phy", "*.phylip"));
		outTreeFileBrowseButton.setOnAction(e -> browseForSaveFile(
				outTreeFileTextField, "Newick tree",
				"*.tre", "*.tree", "*.nwk", "*.newick"));
		treeFileBrowseButton.setOnAction(e -> browseForFile(
				treeFileTextField, "Tree files",
				"*.tre", "*.tree", "*.nwk", "*.newick", "*.nex", "*.nxs"));

		randomiseSeed1Button.setOnAction(e -> randomSeed1TextField.setText(Long.toString(nextSeed())));
		randomiseSeed2Button.setOnAction(e -> randomSeed2TextField.setText(Long.toString(nextSeed())));

		cancelButton.setOnAction(this::onCancel);
		validateButton.setOnAction(this::onValidate);
		previewSettingsButton.setOnAction(this::onPreview);
		runPiclButton.setOnAction(this::onRunPicl);

		autoDetectSpeciesByPrefixButton.setOnAction(this::onAutoDetectSpeciesByPrefix);
		importSpeciesMappingButton.setOnAction(this::onImportSpeciesMapping);

		piclExecutableBrowseButton.setOnAction(e -> browseForFile(
				piclExecutableTextField, "Executable", "*"));
		clearOutputButton.setOnAction(e -> outputTextArea.clear());
		copyOutputButton.setOnAction(this::onCopyOutput);
		stopRunButton.setOnAction(this::onStopRun);

		reloadTreeButton.setOnAction(this::onReloadTree);
		copyTreeButton.setOnAction(this::onCopyTree);
		saveTreeAsButton.setOnAction(this::onSaveTreeAs);
	}

	// =================================================================
	//  UI ⇄ Settings synchronisation
	// =================================================================

	/**
	 * Push values from a Settings instance into the UI.
	 */
	public void applyToUi(Settings s) {
		modelChoiceBox.setValue(s.getModel());
		alignmentFileTextField.setText(s.getAlignmentFile());
		includeAllSitesCheckBox.setSelected(s.isIncludeAllSites());
		thetaTextField.setText(Double.toString(s.getTheta()));
		gammaRateTextField.setText(Double.toString(s.getGammaRate()));
		gammaCategoriesTextField.setText(Integer.toString(s.getGammaCategories()));

		if (s.getStartingTreeSource() == Settings.StartingTreeSource.READ_FROM_FILE)
			readFromTreeFileRadioButton.setSelected(true);
		else
			generateRandomTreeRadioButton.setSelected(true);
		treeFileTextField.setText(s.getTreeFile());
		useBranchLengthsFromTreeCheckBox.setSelected(s.isUseBranchLengthsFromTree());

		branchLengthMethodChoiceBox.setValue(s.getBranchLengthMethod());
		branchLengthIterationsTextField.setText(Long.toString(s.getBranchLengthIterations()));

		treeSearchMethodChoiceBox.setValue(s.getTreeSearchMethod());
		treeSearchIterationsTextField.setText(Long.toString(s.getTreeSearchIterations()));
		coolingRateTextField.setText(Double.toString(s.getCoolingRate()));

		bootstrapReplicatesTextField.setText(Integer.toString(s.getBootstrapReplicates()));
		verboseOutputCheckBox.setSelected(s.isVerboseOutput());
		randomSeed1TextField.setText(Long.toString(s.getRandomSeed1()));
		randomSeed2TextField.setText(Long.toString(s.getRandomSeed2()));

		// Replace the table contents only if the incoming Settings has its own list.
		if (s != this.settings) {
			settings.getLineageAssignments().setAll(s.getLineageAssignments());
		}
		updateCountLabels();
	}

	/**
	 * Read the UI back into the given Settings instance.
	 */
	public void pullFromUi(Settings s) {
		s.setModel(modelChoiceBox.getValue());
		s.setAlignmentFile(alignmentFileTextField.getText());
		s.setIncludeAllSites(includeAllSitesCheckBox.isSelected());
		s.setTheta(parseDouble(thetaTextField, s.getTheta()));
		s.setGammaRate(parseDouble(gammaRateTextField, s.getGammaRate()));
		s.setGammaCategories(parseInt(gammaCategoriesTextField, s.getGammaCategories()));

		s.setStartingTreeSource(readFromTreeFileRadioButton.isSelected()
				? Settings.StartingTreeSource.READ_FROM_FILE
				: Settings.StartingTreeSource.GENERATE_RANDOM);
		s.setTreeFile(treeFileTextField.getText());
		s.setUseBranchLengthsFromTree(useBranchLengthsFromTreeCheckBox.isSelected());

		s.setBranchLengthMethod(branchLengthMethodChoiceBox.getValue());
		s.setBranchLengthIterations(parseLong(branchLengthIterationsTextField, s.getBranchLengthIterations()));

		s.setTreeSearchMethod(treeSearchMethodChoiceBox.getValue());
		s.setTreeSearchIterations(parseLong(treeSearchIterationsTextField, s.getTreeSearchIterations()));
		s.setCoolingRate(parseDouble(coolingRateTextField, s.getCoolingRate()));

		s.setBootstrapReplicates(parseInt(bootstrapReplicatesTextField, s.getBootstrapReplicates()));
		s.setVerboseOutput(verboseOutputCheckBox.isSelected());
		s.setRandomSeed1(parseLong(randomSeed1TextField, s.getRandomSeed1()));
		s.setRandomSeed2(parseLong(randomSeed2TextField, s.getRandomSeed2()));
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
			// Copy scalar state back into our authoritative `settings` instance.
			pullFromUi(this.settings);
			settings.getSpecies().setAll(loaded.getSpecies());
			settings.getLineageAssignments().setAll(loaded.getLineageAssignments());
			lastSettingsFile = file;
			statusLabel.setText("Loaded " + file.getName()
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
			statusLabel.setText("Saved " + file.getName());
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
		var stage = (Stage) cancelButton.getScene().getWindow();
		stage.close();
	}

	private void onValidate(ActionEvent e) {
		pullFromUi(settings);
		var problems = settings.validate();
		if (problems.isEmpty()) {
			statusLabel.setText("Settings OK · " + settings.getLineageAssignments().size() + " lineages");
			new Alert(Alert.AlertType.INFORMATION, "Settings look good.", ButtonType.OK).showAndWait();
		} else {
			statusLabel.setText(problems.size() + " problem(s) found");
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
			statusLabel.setText("Cannot run — " + problems.size() + " problem(s)");
			new Alert(Alert.AlertType.WARNING,
					"Fix these first:\n• " + String.join("\n• ", problems),
					ButtonType.OK).showAndWait();
			return;
		}

		// Resolve the PICL executable.
		var execPath = Paths.get(piclExecutableTextField.getText().trim());
		if (!Files.isExecutable(execPath)) {
			new Alert(Alert.AlertType.ERROR,
					"PICL executable not found or not executable:\n" + execPath,
					ButtonType.OK).showAndWait();
			return;
		}

		// Resolve the alignment file. PICL will fopen() it directly.
		var alignmentText = alignmentFileTextField.getText().trim();
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
		var outTreeText = outTreeFileTextField.getText().trim();
		if (outTreeText.isBlank())
			outTreeText = deriveOutputTreePath(alignmentPath.toString());
		var picltreesPath = Paths.get(outTreeText).toAbsolutePath();

		// Auto-derived siblings.
		var settingsPath = Paths.get(deriveSettingsPath(picltreesPath.toString()));
		var treeInfoPath = Paths.get(deriveTreeInfoPath(picltreesPath.toString()));
		var resultsPath = Paths.get(deriveResultsPath(alignmentPath.toString()));

		// Starting tree file — only consulted by PICL when Random_tree=0.
		// Pass the user's text-field value (resolved); falls back to a
		// sibling of the alignment if blank, so PICL still gets a path.
		var treeFileText = treeFileTextField.getText().trim();
		Path treeFilePath;
		if (treeFileText.isBlank()) {
			treeFilePath = alignmentPath.resolveSibling("treefile.tre");
		} else {
			var tfp = Paths.get(treeFileText);
			treeFilePath = tfp.isAbsolute()
					? tfp
					: alignmentPath.resolveSibling(tfp).toAbsolutePath();
		}

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

		// Switch the user to the Output tab and clear previous output.
		mainTabPane.getSelectionModel().select(outputTab);
		outputTextArea.clear();

		appendOutput("$ " + execPath
					 + " " + settingsPath
					 + " " + alignmentPath
					 + " " + treeFilePath
					 + " " + treeInfoPath
					 + " " + picltreesPath
					 + " " + resultsPath + "\n");
		appendOutput("(working directory: " + workDir + ")\n\n");

		// The C side accepts up to 6 positional args:
		//   argv[1] = settings
		//   argv[2] = data.phy (alignment)
		//   argv[3] = treefile.tre (input starting tree, if Random_tree=0)
		//   argv[4] = outtree.tre
		//   argv[5] = picltrees.tre (the user's Output tree — Newick only)
		//   argv[6] = results
		var pb = new ProcessBuilder(
				execPath.toString(),
				settingsPath.toString(),
				alignmentPath.toString(),
				treeFilePath.toString(),
				treeInfoPath.toString(),
				picltreesPath.toString(),
				resultsPath.toString())
				.directory(workDir)
				.redirectErrorStream(true);

		// Remember the picltrees path so onProcessExited can read it
		// back into the Tree tab (this is the Newick-only file).
		this.pendingOutTreePath = picltreesPath;
		try {
			currentProcess = pb.start();
		} catch (IOException ex) {
			error("Could not launch PICL", ex);
			return;
		}

		running.set(true);
		runStatusLabel.setText("Running…");
		statusLabel.setText("PICL is running");

		var process = currentProcess;
		var reader = new Thread(() -> streamProcessOutput(process), "picl-output-reader");
		reader.setDaemon(true);
		reader.start();
	}

	/**
	 * Reads the process's combined stdout/stderr line by line on the calling
	 * thread and pumps each line back to the FX thread. Updates UI state when
	 * the process exits.
	 */
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
		if (exitCode == 0) {
			runStatusLabel.setText("Finished");
			statusLabel.setText("PICL finished successfully");

			// Read back the tree from wherever we asked picl to write it.
			if (pendingOutTreePath != null
				&& loadTreeFromFile(pendingOutTreePath)) {
				mainTabPane.getSelectionModel().select(treeTab);
			}
		} else {
			runStatusLabel.setText("Failed (exit " + exitCode + ")");
			statusLabel.setText("PICL exited with code " + exitCode);
		}
	}

	/**
	 * Reads the given tree file into the Tree tab's TextArea.
	 * Returns true on success, false (with a status update) if the file is
	 * missing or unreadable.
	 */
	private boolean loadTreeFromFile(Path treeFile) {
		if (!Files.isReadable(treeFile)) {
			treeFilePathLabel.setText(treeFile + "  (not found)");
			return false;
		}
		try {
			var content = Files.readString(treeFile);
			treeTextArea.setText(content);
			treeFilePathLabel.setText(treeFile.toString());
			lastTreeFile = treeFile;
			return true;
		} catch (IOException ex) {
			treeFilePathLabel.setText(treeFile + "  (error)");
			statusLabel.setText("Could not read " + treeFile.getFileName() + ": " + ex.getMessage());
			return false;
		}
	}

	private void onStopRun(ActionEvent e) {
		var p = currentProcess;
		if (p == null || !p.isAlive()) return;
		appendOutput("\n[stopping picl…]\n");
		p.destroy();
		// Give it a moment, then force.
		new Thread(() -> {
			try {
				p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
			}
			if (p.isAlive()) p.destroyForcibly();
		}, "picl-stop").start();
	}

	private void onCopyOutput(ActionEvent e) {
		var content = new ClipboardContent();
		content.putString(outputTextArea.getText());
		Clipboard.getSystemClipboard().setContent(content);
		statusLabel.setText("Output copied to clipboard");
	}

	private void onReloadTree(ActionEvent e) {
		// Prefer the most recently loaded tree path, then the current
		// value of the Output tree field.
		Path target;
		if (lastTreeFile != null) {
			target = lastTreeFile;
		} else {
			var fieldText = outTreeFileTextField.getText().trim();
			if (fieldText.isBlank()) {
				statusLabel.setText("No tree to reload — run PICL first.");
				return;
			}
			target = Paths.get(fieldText);
		}
		if (loadTreeFromFile(target)) {
			statusLabel.setText("Reloaded " + target.getFileName());
		}
	}

	private void onCopyTree(ActionEvent e) {
		var content = new ClipboardContent();
		content.putString(treeTextArea.getText());
		Clipboard.getSystemClipboard().setContent(content);
		statusLabel.setText("Tree copied to clipboard");
	}

	private void onSaveTreeAs(ActionEvent e) {
		var chooser = new FileChooser();
		chooser.setTitle("Save tree as");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Newick tree", "*.tre", "*.tree", "*.nwk", "*.newick"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
		if (lastTreeFile != null && lastTreeFile.getParent() != null) {
			chooser.setInitialDirectory(lastTreeFile.getParent().toFile());
			chooser.setInitialFileName(lastTreeFile.getFileName().toString());
		} else {
			chooser.setInitialFileName("output.tre");
		}
		var file = chooser.showSaveDialog(window());
		if (file == null) return;
		try {
			Files.writeString(file.toPath(), treeTextArea.getText());
			statusLabel.setText("Tree saved to " + file.getName());
		} catch (IOException ex) {
			error("Could not save tree", ex);
		}
	}

	/**
	 * Append text on the FX thread (caller is responsible for thread context).
	 */
	private void appendOutput(String text) {
		outputTextArea.appendText(text);
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
		// Refresh the explicit species list to match what we just assigned.
		settings.getSpecies().setAll(seen);
		lineageSpeciesTableView.refresh();
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
			for (var line : java.nio.file.Files.readAllLines(file.toPath())) {
				if (line.isBlank() || line.startsWith("#")) continue;
				var parts = line.split("\t", 2);
				if (parts.length < 2) continue;
				var la = byName.get(parts[0]);
				if (la != null) la.setSpecies(parts[1]);
			}
			lineageSpeciesTableView.refresh();
			updateCountLabels();
			statusLabel.setText("Mapping imported from " + file.getName());
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

	private javafx.stage.Window window() {
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
		statusLabel.setText(header + ": " + t.getMessage());
		var a = new Alert(Alert.AlertType.ERROR, t.getMessage() == null ? t.toString() : t.getMessage(),
				ButtonType.OK);
		a.setHeaderText(header);
		a.showAndWait();
	}
}
