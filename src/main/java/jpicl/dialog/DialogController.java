/*
 * DialogController.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import jpicl.main.Version;

import static jpicl.util.PiclExtractor.platformKey;

/**
 * FXML controller for the PICL settings dialog. It owns the @FXML-injected nodes
 * and exposes them via getters. All wiring, event handlers, and
 * run-time state live in {@link DialogPresenter}.
 * Pattern: this is the "passive view"-style split — the controller is
 * a typed bag of nodes, the presenter does the work.
 */
public class DialogController {
	@FXML
	private Button alignmentBrowseButton;
	@FXML
	private TextField alignmentFileTextField;
	@FXML
	private Button clearSpeciesListButton;
	@FXML
	private Button lineagesFromDataButton;
	@FXML
	private Button autoDetectSpeciesByPrefixButton;
	@FXML
	private Label bootstrapReplicatesHintLabel;
	@FXML
	private TextField bootstrapReplicatesTextField;
	@FXML
	private TitledPane bootstrapSeedsOutputTitledPane;
	@FXML
	private TextField branchLengthIterationsTextField;
	@FXML
	private ChoiceBox<Settings.BranchLengthMethod> branchLengthMethodChoiceBox;
	@FXML
	private Label branchLengthOptimisationHintLabel;
	@FXML
	private TitledPane branchLengthOptimisationTitledPane;
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
	private Menu windowMenu;
	@FXML
	private MenuItem aboutMenuItem;
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
	private TitledPane modelAndDataTitledPane;
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
	private TitledPane speciesAndLineagesTitledPane;
	@FXML
	private TableColumn<Settings.LineageAssignment, String> speciesAssignmentColumn;
	@FXML
	private Label speciesCountLabel;
	@FXML
	private TitledPane startingTreeTitledPane;
	@FXML
	private ToggleGroup startingTreeToggleGroup;
	@FXML
	private Label statusLabel;
	@FXML
	private Label thetaHintLabel;
	@FXML
	private TextField thetaTextField;
	@FXML
	private Button treeFileBrowseButton;
	@FXML
	private TextField treeFileTextField;
	@FXML
	private TextField treeSearchIterationsTextField;
	@FXML
	private TextField multiIterTextField;
	@FXML
	private TextField probBoundTextField;
	@FXML
	private TextField testIncrTextField;
	@FXML
	private TextField optSlopeTextField;
	@FXML
	private ChoiceBox<Settings.TreeSearchMethod> treeSearchMethodChoiceBox;
	@FXML
	private TitledPane treeSearchTitledPane;
	@FXML
	private CheckBox useBranchLengthsFromTreeCheckBox;
	@FXML
	private Button validateButton;
	@FXML
	private CheckBox verboseOutputCheckBox;

	@FXML
	private TextField outTreeFileTextField;
	@FXML
	private Button outTreeFileBrowseButton;
	@FXML
	private TitledPane moreFilesTitledPane;
	@FXML
	private Label settingsPathLabel;
	@FXML
	private Label treesPathLabel;
	@FXML
	private Label valuesPathLabel;
	@FXML
	private Label logPathLabel;
	@FXML
	private Label bootstrapPathLabel;

	@FXML
	private TabPane mainTabPane;
	@FXML
	private Tab settingsTab;
	@FXML
	private Tab logTab;
	@FXML
	private BorderPane logBorderPane;
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
	private Tab outputTab;
	@FXML
	private Menu fileMenu;
	@FXML
	private Label treesFilePathLabel;
	@FXML
	private Button reloadTreeButton;
	@FXML
	private Button copyTreeButton;
	@FXML
	private Button saveTreeAsButton;
	@FXML
	private TextArea outputTextArea;

	@FXML
	private Tab treeTab;
	@FXML
	private Label treeFilePathLabel;
	@FXML
	private Pane treeCanvasPane;

	@FXML
	private MenuBar menuBar;
	@FXML
	private MenuItem newMenuItem;
	@FXML
	private MenuItem openMenuItem;
	@FXML
	private MenuItem exportSettingsMenuItem;
	@FXML
	private MenuItem importSettingsMenuItem;
	@FXML
	private MenuItem printMenuItem;
	@FXML
	private MenuItem pageSetupMenuItem;
	@FXML
	private MenuItem closeMenuItem;
	@FXML
	private MenuItem quitMenuItem;
	@FXML
	private MenuItem undoMenuItem;
	@FXML
	private MenuItem redoMenuItem;
	@FXML
	private MenuItem cutMenuItem;
	@FXML
	private MenuItem copyMenuItem;
	@FXML
	private MenuItem pasteMenuItem;
	@FXML
	private MenuItem deleteMenuItem;
	@FXML
	private Menu viewMenu;
	@FXML
	private CheckMenuItem fullScreenMenuItem;
	@FXML
	private CheckMenuItem darkModeMenuItem;
	@FXML
	private ToggleGroup tabsToggleGroup;
	@FXML
	private RadioMenuItem settingsTabMenuItem;
	@FXML
	private RadioMenuItem logTabMenuItem;
	@FXML
	private RadioMenuItem outputTabMenuItem;
	@FXML
	private RadioMenuItem treeTabMenuItem;
	@FXML
	private MenuItem checkForUpdatesMenuItem;
	@FXML
	private MenuItem openGitHubMenuItem;

	private final LogView logView = new LogView();

	@FXML
	private void initialize() {
		if (platformKey().startsWith("macos")) {
			getMenuBar().setUseSystemMenuBar(true);
			fileMenu.getItems().remove(quitMenuItem);
		}
		logBorderPane.setCenter(logView);
		logView.appendLine(Version.SHORT_DESCRIPTION + "\n");
		setupToolTips();
	}

	private void setupToolTips() {

		modelChoiceBox.setTooltip(new Tooltip("""
				Model:
				1 = multilocus or CIS data
				2 = multilocus or CIS data with discrete gamma-distributed rate variation
				3 = SNP data
				4 = JC69 model for gene trees (composite likelihood under the JC69 model without the multispecies coalescent)
				"""));

		includeAllSitesCheckBox.setTooltip(new Tooltip("""
				Gap:
				0 = sites containing a gap in at least one lineage are ignored
				1 = all sites are included; gapped sites are currently ignored at the quartet level
				"""));

		bootstrapReplicatesTextField.setTooltip(new Tooltip("""
				Bootstrap:
				If 0, no bootstrapping is done.
				If >0, specifies the number of bootstrap replicates used to obtain confidence intervals.
				Opt_bl must not be 0 when bootstrapping is requested.
				"""));

		thetaTextField.setTooltip(new Tooltip("""
				Theta:
				Effective population size parameter (4Nₑμ).
				Currently this must be provided by the user as a fixed value.
				"""));

		gammaRateTextField.setTooltip(new Tooltip("""
				Rate_param:
				Rate parameter in the discrete gamma model of rate variation.
				Optimized automatically if branch lengths or tree search are optimized.
				"""));

		readFromTreeFileRadioButton.setTooltip(new Tooltip("""
				Random_tree:
				0 = read starting tree from treefile.tre
				1 = generate a random starting tree
				"""));

		generateRandomTreeRadioButton.setTooltip(new Tooltip("""
				Random_tree:
				0 = read starting tree from treefile.tre
				1 = generate a random starting tree
				"""));

		branchLengthMethodChoiceBox.setTooltip(new Tooltip("""
				Opt_bl:
				0 = no branch length optimization (composite likelihood will be evaluated for
				 the user-specified (if Random_tree = 0)
				 or randomly generated (if Random_tree = 1) tree)
				1 = uphill optimization
				2 = simulated annealing optimization
				3 = numerical derivatives (not yet implemented)
				"""));

		useBranchLengthsFromTreeCheckBox.setTooltip(new Tooltip("""
				User_bl:
				1 = use branch lengths from the input tree as starting values
				0 = evenly spaced branch lengths
				Ignored if Opt_bl = 0.
				"""));

		branchLengthIterationsTextField.setTooltip(new Tooltip("""
				Num_opt:
				Number of optimization iterations used for branch length optimization.
				Used when Opt_bl = 1 or 2.
				"""));

		randomSeed1TextField.setTooltip(new Tooltip("""
				Seed1:
				Random number seed used for repeatability of analyses.
				"""));

		randomSeed2TextField.setTooltip(new Tooltip("""
				Seed2:
				Random number seed used for repeatability of analyses.
				"""));

		gammaCategoriesTextField.setTooltip(new Tooltip("""
				Num_cat:
				Number of categories in the discrete gamma model.
				Used only when Model = 2.
				"""));

		treeSearchMethodChoiceBox.setTooltip(new Tooltip("""
				Tree_search:
				0 = none (user specified or randomly generated tree will be evaluated)
				1 = uphill search using NNI proposals
				2 = simulated annealing search using NNI proposals
				"""));

		treeSearchIterationsTextField.setTooltip(new Tooltip("""
				Num_iter:
				Number of iterations for the tree search.
				Used for both search methods.
				"""));

		probBoundTextField.setTooltip(new Tooltip("""
				Prob_bound:
				Probability that a node has NOT been selected for rearrangement.
				Used as a stopping rule in annealing tree search.
				Recommended value: 0.05
				Ignored unless Tree_search = 2.
				"""));

		testIncrTextField.setTooltip(new Tooltip("""
				Test_incr:
				Frequency with which the cooling schedule is updated
				in adaptive annealing.
				Ignored unless Tree_search = 2.
				"""));

		optSlopeTextField.setTooltip(new Tooltip("""
				Opt_slope:
				Target value for adaptive annealing.
				Recommended value: -0.01
				Usually should not be modified.
				Ignored unless Tree_search = 2.
				"""));

		coolingRateTextField.setTooltip(new Tooltip("""
				Beta:
				Parameter controlling the cooling rate in simulated annealing.
				Ignored if Tree_search = 0.
				"""));

		verboseOutputCheckBox.setTooltip(new Tooltip("""
				Verbose:
				0 = normal output
				1 = print additional debugging and checking information
				Normal users should generally leave this disabled.
				"""));

		lineageSpeciesTableView.setTooltip(new Tooltip("""
				Species and lineage mapping:
				The number of species is followed by a list of species names.
				Each remaining line assigns a lineage to a species:
				first word  = species name
				second word = lineage name exactly as it appears in the alignment file
				Lineages may appear in any order.
				"""));
	}


	public Button getAlignmentBrowseButton() {
		return alignmentBrowseButton;
	}

	public TextField getAlignmentFileTextField() {
		return alignmentFileTextField;
	}

	public Button getClearSpeciesListButton() {
		return clearSpeciesListButton;
	}

	public Button getLineagesFromDataButton() {
		return lineagesFromDataButton;
	}

	public Button getAutoDetectSpeciesByPrefixButton() {
		return autoDetectSpeciesByPrefixButton;
	}

	public Label getBootstrapReplicatesHintLabel() {
		return bootstrapReplicatesHintLabel;
	}

	public TextField getBootstrapReplicatesTextField() {
		return bootstrapReplicatesTextField;
	}

	public TitledPane getBootstrapSeedsOutputTitledPane() {
		return bootstrapSeedsOutputTitledPane;
	}

	public TextField getBranchLengthIterationsTextField() {
		return branchLengthIterationsTextField;
	}

	public ChoiceBox<Settings.BranchLengthMethod> getBranchLengthMethodChoiceBox() {
		return branchLengthMethodChoiceBox;
	}

	public Label getBranchLengthOptimisationHintLabel() {
		return branchLengthOptimisationHintLabel;
	}

	public TitledPane getBranchLengthOptimisationTitledPane() {
		return branchLengthOptimisationTitledPane;
	}

	public Button getCancelButton() {
		return cancelButton;
	}

	public Label getCoolingRateHintLabel() {
		return coolingRateHintLabel;
	}

	public TextField getCoolingRateTextField() {
		return coolingRateTextField;
	}

	public Label getGammaCategoriesHintLabel() {
		return gammaCategoriesHintLabel;
	}

	public TextField getGammaCategoriesTextField() {
		return gammaCategoriesTextField;
	}

	public Label getGammaRateHintLabel() {
		return gammaRateHintLabel;
	}

	public TextField getGammaRateTextField() {
		return gammaRateTextField;
	}

	public RadioButton getGenerateRandomTreeRadioButton() {
		return generateRandomTreeRadioButton;
	}

	public Button getImportSpeciesMappingButton() {
		return importSpeciesMappingButton;
	}

	public CheckBox getIncludeAllSitesCheckBox() {
		return includeAllSitesCheckBox;
	}

	public MenuItem getAboutMenuItem() {
		return aboutMenuItem;
	}

	public Menu getWindowMenu() {
		return windowMenu;
	}

	public TableColumn<Settings.LineageAssignment, Integer> getLineageIndexColumn() {
		return lineageIndexColumn;
	}

	public TableColumn<Settings.LineageAssignment, String> getLineageNameColumn() {
		return lineageNameColumn;
	}

	public TableView<Settings.LineageAssignment> getLineageSpeciesTableView() {
		return lineageSpeciesTableView;
	}

	public Label getLineagesCountLabel() {
		return lineagesCountLabel;
	}

	public Button getLoadSettingsButton() {
		return loadSettingsButton;
	}

	public TitledPane getModelAndDataTitledPane() {
		return modelAndDataTitledPane;
	}

	public ChoiceBox<Settings.Model> getModelChoiceBox() {
		return modelChoiceBox;
	}

	public Button getPreviewSettingsButton() {
		return previewSettingsButton;
	}

	public TextField getRandomSeed1TextField() {
		return randomSeed1TextField;
	}

	public TextField getRandomSeed2TextField() {
		return randomSeed2TextField;
	}

	public Button getRandomiseSeed1Button() {
		return randomiseSeed1Button;
	}

	public Button getRandomiseSeed2Button() {
		return randomiseSeed2Button;
	}

	public RadioButton getReadFromTreeFileRadioButton() {
		return readFromTreeFileRadioButton;
	}

	public Button getRunPiclButton() {
		return runPiclButton;
	}

	public TitledPane getSpeciesAndLineagesTitledPane() {
		return speciesAndLineagesTitledPane;
	}

	public TableColumn<Settings.LineageAssignment, String> getSpeciesAssignmentColumn() {
		return speciesAssignmentColumn;
	}

	public Label getSpeciesCountLabel() {
		return speciesCountLabel;
	}

	public TitledPane getStartingTreeTitledPane() {
		return startingTreeTitledPane;
	}

	public ToggleGroup getStartingTreeToggleGroup() {
		return startingTreeToggleGroup;
	}

	public Label getStatusLabel() {
		return statusLabel;
	}

	public Label getThetaHintLabel() {
		return thetaHintLabel;
	}

	public TextField getThetaTextField() {
		return thetaTextField;
	}

	public Button getTreeFileBrowseButton() {
		return treeFileBrowseButton;
	}

	public TextField getTreeFileTextField() {
		return treeFileTextField;
	}

	public TextField getTreeSearchIterationsTextField() {
		return treeSearchIterationsTextField;
	}

	public TextField getMultiIterTextField() {
		return multiIterTextField;
	}

	public TextField getProbBoundTextField() {
		return probBoundTextField;
	}

	public TextField getTestIncrTextField() {
		return testIncrTextField;
	}

	public TextField getOptSlopeTextField() {
		return optSlopeTextField;
	}

	public ChoiceBox<Settings.TreeSearchMethod> getTreeSearchMethodChoiceBox() {
		return treeSearchMethodChoiceBox;
	}

	public TitledPane getTreeSearchTitledPane() {
		return treeSearchTitledPane;
	}

	public CheckBox getUseBranchLengthsFromTreeCheckBox() {
		return useBranchLengthsFromTreeCheckBox;
	}

	public Button getValidateButton() {
		return validateButton;
	}

	public CheckBox getVerboseOutputCheckBox() {
		return verboseOutputCheckBox;
	}

	public TextField getOutTreeFileTextField() {
		return outTreeFileTextField;
	}

	public Button getOutTreeFileBrowseButton() {
		return outTreeFileBrowseButton;
	}

	public TitledPane getMoreFilesTitledPane() {
		return moreFilesTitledPane;
	}

	public Label getSettingsPathLabel() {
		return settingsPathLabel;
	}

	public Label getTreesPathLabel() {
		return treesPathLabel;
	}

	public Label getValuesPathLabel() {
		return valuesPathLabel;
	}

	public Label getLogPathLabel() {
		return logPathLabel;
	}

	public Label getBootstrapPathLabel() {
		return bootstrapPathLabel;
	}

	public TabPane getMainTabPane() {
		return mainTabPane;
	}

	public Tab getSettingsTab() {
		return settingsTab;
	}

	public Tab getLogTab() {
		return logTab;
	}

	public TextField getPiclExecutableTextField() {
		return piclExecutableTextField;
	}

	public Button getPiclExecutableBrowseButton() {
		return piclExecutableBrowseButton;
	}

	public Label getRunStatusLabel() {
		return runStatusLabel;
	}

	public ProgressBar getRunProgressBar() {
		return runProgressBar;
	}

	public Button getClearOutputButton() {
		return clearOutputButton;
	}

	public Button getCopyOutputButton() {
		return copyOutputButton;
	}

	public Button getStopRunButton() {
		return stopRunButton;
	}

	public LogView getLogView() {
		return logView;
	}

	public Tab getOutputTab() {
		return outputTab;
	}

	public Label getTreesFilePathLabel() {
		return treesFilePathLabel;
	}

	public Button getReloadTreeButton() {
		return reloadTreeButton;
	}

	public Button getCopyTreeButton() {
		return copyTreeButton;
	}

	public Button getSaveTreeAsButton() {
		return saveTreeAsButton;
	}

	public TextArea getOutputTextArea() {
		return outputTextArea;
	}

	public Tab getTreeTab() {
		return treeTab;
	}

	public Label getTreeFilePathLabel() {
		return treeFilePathLabel;
	}

	public Pane getTreeCanvasPane() {
		return treeCanvasPane;
	}

	public MenuBar getMenuBar() {
		return menuBar;
	}

	public MenuItem getNewMenuItem() {
		return newMenuItem;
	}

	public MenuItem getOpenMenuItem() {
		return openMenuItem;
	}

	public MenuItem getExportSettingsMenuItem() {
		return exportSettingsMenuItem;
	}

	public MenuItem getImportSettingsMenuItem() {
		return importSettingsMenuItem;
	}

	public MenuItem getPrintMenuItem() {
		return printMenuItem;
	}

	public MenuItem getPageSetupMenuItem() {
		return pageSetupMenuItem;
	}

	public MenuItem getCloseMenuItem() {
		return closeMenuItem;
	}

	public MenuItem getQuitMenuItem() {
		return quitMenuItem;
	}

	public MenuItem getUndoMenuItem() {
		return undoMenuItem;
	}

	public MenuItem getRedoMenuItem() {
		return redoMenuItem;
	}

	public MenuItem getCutMenuItem() {
		return cutMenuItem;
	}

	public MenuItem getCopyMenuItem() {
		return copyMenuItem;
	}

	public MenuItem getPasteMenuItem() {
		return pasteMenuItem;
	}

	public MenuItem getDeleteMenuItem() {
		return deleteMenuItem;
	}

	public Menu getViewMenu() {
		return viewMenu;
	}

	public CheckMenuItem getFullScreenMenuItem() {
		return fullScreenMenuItem;
	}

	public CheckMenuItem getDarkModeMenuItem() {
		return darkModeMenuItem;
	}

	public ToggleGroup getTabsToggleGroup() {
		return tabsToggleGroup;
	}

	public RadioMenuItem getSettingsTabMenuItem() {
		return settingsTabMenuItem;
	}

	public RadioMenuItem getLogTabMenuItem() {
		return logTabMenuItem;
	}

	public RadioMenuItem getOutputTabMenuItem() {
		return outputTabMenuItem;
	}

	public RadioMenuItem getTreeTabMenuItem() {
		return treeTabMenuItem;
	}

	public MenuItem getCheckForUpdatesMenuItem() {
		return checkForUpdatesMenuItem;
	}

	public MenuItem getOpenGitHubMenuItem() {
		return openGitHubMenuItem;
	}
}
