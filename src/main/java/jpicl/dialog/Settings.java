package jpicl.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Backing model for the PICL settings dialog.
 * <p>
 * Defines the closed-set choice enums shown in the ChoiceBoxes and radio
 * buttons, the row type for the species/lineages TableView, and the scalar
 * parameters with sensible defaults from the mockup.
 * <p>
 * {@link #read(Path)} and {@link #write(Path)} use a simple {@code key = value}
 * format as a placeholder. Replace the bodies of those two methods with
 * PICL's real settings-file format once a sample is available — nothing else
 * in the controller needs to change.
 */
public class Settings {

	// =================================================================
	//  Enums for the three ChoiceBox<?> controls
	// =================================================================

	/**
	 * Substitution / coalescent model. PICL codes 1 = CIS, 2 = CIS+gamma.
	 */
	public enum Model {
		CIS(1, "Multilocus / CIS"),
		CIS_GAMMA(2, "Multilocus / CIS with gamma");

		private final int code;
		private final String displayName;

		Model(int code, String displayName) {
			this.code = code;
			this.displayName = displayName;
		}

		public int code() {
			return code;
		}

		public String displayName() {
			return displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}

		public static Model fromCode(int code) {
			for (var v : values()) if (v.code == code) return v;
			throw new IllegalArgumentException("Unknown Model code: " + code);
		}
	}

	/**
	 * Branch-length optimisation method. PICL codes 1 = Uphill,
	 * 3 = numerical derivatives (currently not implemented in PICL).
	 */
	public enum BranchLengthMethod {
		UPHILL(1, "Uphill", true),
		NUMERICAL_DERIVATIVES(3, "Numerical derivatives", false);

		private final int code;
		private final String displayName;
		private final boolean implemented;

		BranchLengthMethod(int code, String displayName, boolean implemented) {
			this.code = code;
			this.displayName = displayName;
			this.implemented = implemented;
		}

		public int code() {
			return code;
		}

		public String displayName() {
			return displayName;
		}

		public boolean isImplemented() {
			return implemented;
		}

		@Override
		public String toString() {
			return displayName;
		}

		public static BranchLengthMethod fromCode(int code) {
			for (var v : values()) if (v.code == code) return v;
			throw new IllegalArgumentException("Unknown BranchLengthMethod code: " + code);
		}
	}

	/**
	 * Tree-search method. PICL codes 1 = NNI, 2 = simulated-annealing NNI.
	 */
	public enum TreeSearchMethod {
		NNI(1, "NNI", false),
		SA_NNI(2, "Simulated annealing NNI", true);

		private final int code;
		private final String displayName;
		private final boolean usesCoolingRate;

		TreeSearchMethod(int code, String displayName, boolean usesCoolingRate) {
			this.code = code;
			this.displayName = displayName;
			this.usesCoolingRate = usesCoolingRate;
		}

		public int code() {
			return code;
		}

		public String displayName() {
			return displayName;
		}

		public boolean usesCoolingRate() {
			return usesCoolingRate;
		}

		@Override
		public String toString() {
			return displayName;
		}

		public static TreeSearchMethod fromCode(int code) {
			for (var v : values()) if (v.code == code) return v;
			throw new IllegalArgumentException("Unknown TreeSearchMethod code: " + code);
		}
	}

	/**
	 * Backs the two RadioButtons in the startingTreeToggleGroup.
	 */
	public enum StartingTreeSource {READ_FROM_FILE, GENERATE_RANDOM}

	// =================================================================
	//  Row type for the species/lineages TableView
	//  (species are user-defined per alignment, so not an enum)
	// =================================================================

	public static class LineageAssignment {
		private final int index;
		private String lineage;
		private String species;

		public LineageAssignment(int index, String lineage, String species) {
			this.index = index;
			this.lineage = lineage;
			this.species = species;
		}

		public int getIndex() {
			return index;
		}

		public String getLineage() {
			return lineage;
		}

		/**
		 * Replaces the lineage name. Used by the FASTA-input path to
		 * temporarily swap in placeholder names while writing the
		 * settings file PICL reads, then restore originals.
		 */
		public void setLineage(String lineage) {
			this.lineage = lineage;
		}

		public String getSpecies() {
			return species;
		}

		public void setSpecies(String species) {
			this.species = species;
		}
	}

	// =================================================================
	//  Scalar parameter fields (defaults match the mockup)
	// =================================================================

	private Model model = Model.CIS_GAMMA;
	private String alignmentFile = "";
	private boolean includeAllSites = true;
	private double theta = 0.002;
	private double gammaRate = 1.0;
	private int gammaCategories = 4;

	private StartingTreeSource startingTreeSource = StartingTreeSource.GENERATE_RANDOM;
	private String treeFile = "";
	private boolean useBranchLengthsFromTree = true;

	private BranchLengthMethod branchLengthMethod = BranchLengthMethod.UPHILL;
	private long branchLengthIterations = 1_000_000L;

	private TreeSearchMethod treeSearchMethod = TreeSearchMethod.SA_NNI;
	private long treeSearchIterations = 30_000L;
	private int multiIter = 3;
	private double probBound = 0.05;
	private int testIncr = 250;
	private double optSlope = -0.01;
	private double coolingRate = 0.005;

	private int bootstrapReplicates = 0;
	private boolean verboseOutput = true;
	private long randomSeed1 = 12345L;
	private long randomSeed2 = 67890L;

	/**
	 * Ordered list of species names — written as the second post-header line.
	 */
	private final ObservableList<String> species = FXCollections.observableArrayList();

	private final ObservableList<LineageAssignment> lineageAssignments =
			FXCollections.observableArrayList();

	// =================================================================
	//  Getters / setters
	// =================================================================

	public Model getModel() {
		return model;
	}

	public void setModel(Model v) {
		this.model = v;
	}

	public String getAlignmentFile() {
		return alignmentFile;
	}

	public void setAlignmentFile(String v) {
		this.alignmentFile = v;
	}

	public boolean isIncludeAllSites() {
		return includeAllSites;
	}

	public void setIncludeAllSites(boolean v) {
		this.includeAllSites = v;
	}

	public double getTheta() {
		return theta;
	}

	public void setTheta(double v) {
		this.theta = v;
	}

	public double getGammaRate() {
		return gammaRate;
	}

	public void setGammaRate(double v) {
		this.gammaRate = v;
	}

	public int getGammaCategories() {
		return gammaCategories;
	}

	public void setGammaCategories(int v) {
		this.gammaCategories = v;
	}

	public StartingTreeSource getStartingTreeSource() {
		return startingTreeSource;
	}

	public void setStartingTreeSource(StartingTreeSource v) {
		this.startingTreeSource = v;
	}

	public String getTreeFile() {
		return treeFile;
	}

	public void setTreeFile(String v) {
		this.treeFile = v;
	}

	public boolean isUseBranchLengthsFromTree() {
		return useBranchLengthsFromTree;
	}

	public void setUseBranchLengthsFromTree(boolean v) {
		this.useBranchLengthsFromTree = v;
	}

	public BranchLengthMethod getBranchLengthMethod() {
		return branchLengthMethod;
	}

	public void setBranchLengthMethod(BranchLengthMethod v) {
		this.branchLengthMethod = v;
	}

	public long getBranchLengthIterations() {
		return branchLengthIterations;
	}

	public void setBranchLengthIterations(long v) {
		this.branchLengthIterations = v;
	}

	public TreeSearchMethod getTreeSearchMethod() {
		return treeSearchMethod;
	}

	public void setTreeSearchMethod(TreeSearchMethod v) {
		this.treeSearchMethod = v;
	}

	public long getTreeSearchIterations() {
		return treeSearchIterations;
	}

	public void setTreeSearchIterations(long v) {
		this.treeSearchIterations = v;
	}

	public int getMultiIter() {
		return multiIter;
	}

	public void setMultiIter(int v) {
		this.multiIter = v;
	}

	public double getProbBound() {
		return probBound;
	}

	public void setProbBound(double v) {
		this.probBound = v;
	}

	public int getTestIncr() {
		return testIncr;
	}

	public void setTestIncr(int v) {
		this.testIncr = v;
	}

	public double getOptSlope() {
		return optSlope;
	}

	public void setOptSlope(double v) {
		this.optSlope = v;
	}

	public double getCoolingRate() {
		return coolingRate;
	}

	public void setCoolingRate(double v) {
		this.coolingRate = v;
	}

	public int getBootstrapReplicates() {
		return bootstrapReplicates;
	}

	public void setBootstrapReplicates(int v) {
		this.bootstrapReplicates = v;
	}

	public boolean isVerboseOutput() {
		return verboseOutput;
	}

	public void setVerboseOutput(boolean v) {
		this.verboseOutput = v;
	}

	public long getRandomSeed1() {
		return randomSeed1;
	}

	public void setRandomSeed1(long v) {
		this.randomSeed1 = v;
	}

	public long getRandomSeed2() {
		return randomSeed2;
	}

	public void setRandomSeed2(long v) {
		this.randomSeed2 = v;
	}

	public ObservableList<String> getSpecies() {
		return species;
	}

	public ObservableList<LineageAssignment> getLineageAssignments() {
		return lineageAssignments;
	}

	// =================================================================
	//  PICL settings file I/O
	//
	//  Format (positional, line-based):
	//
	//      Model: <int>
	//      Gaps: <0|1>
	//      Bootstrap: <int>
	//      Theta: <double>
	//      Rate_param: <double>
	//      Random_tree: <0|1>          1 = generate, 0 = read from file
	//      Opt_bl: <int>               branch-length method code
	//      User_bl: <0|1>              use branch lengths from user tree
	//      Num_opt: <long>             branch-length iterations
	//      Seed1: <long>
	//      Seed2: <long>
	//      Num_cat: <int>
	//      Tree_search: <int>          tree-search method code
	//      Num_iter: <long>
	//      Multi_iter: <int>           multi-start iterations
	//      Prob_bound: <double>        acceptance-probability bound
	//      Test_incr: <int>            test increment
	//      Opt_slope: <double>         optimisation slope threshold
	//      Beta: <double>
	//      Verbose: <0|1>
	//      <speciesCount>
	//      <space-separated species names>
	//      <species> <lineage>         repeated, one line per lineage
	// =================================================================

	/**
	 * PICL settings-file key names (header section).
	 */
	public static final class Key {
		public static final String MODEL = "Model";
		public static final String GAPS = "Gaps";          // 1 = include all sites
		public static final String BOOTSTRAP = "Bootstrap";
		public static final String THETA = "Theta";
		public static final String RATE_PARAM = "Rate_param";    // gamma rate
		public static final String RANDOM_TREE = "Random_tree";   // 1 = generate, 0 = read
		public static final String OPT_BL = "Opt_bl";        // branch-length method code
		public static final String USER_BL = "User_bl";       // use branch lengths from tree
		public static final String NUM_OPT = "Num_opt";       // branch-length iterations
		public static final String SEED1 = "Seed1";
		public static final String SEED2 = "Seed2";
		public static final String NUM_CAT = "Num_cat";       // gamma categories
		public static final String TREE_SEARCH = "Tree_search";   // tree-search method code
		public static final String NUM_ITER = "Num_iter";      // tree-search iterations
		public static final String MULTI_ITER = "Multi_iter";   // multi-start iterations
		public static final String PROB_BOUND = "Prob_bound";   // acceptance probability bound
		public static final String TEST_INCR = "Test_incr";    // test increment
		public static final String OPT_SLOPE = "Opt_slope";    // optimisation slope threshold
		public static final String BETA = "Beta";          // cooling rate
		public static final String VERBOSE = "Verbose";

		private Key() {
		}
	}

	/**
	 * Reads a PICL settings file and returns a populated Settings instance.
	 */
	public static Settings read(Path path) throws IOException {
		var settings = new Settings();
		var lines = Files.readAllLines(path);
		int i = 0;

		// ----- header section: "Key: value" lines -----
		while (i < lines.size()) {
			var line = lines.get(i).trim();
			if (line.isEmpty()) {
				i++;
				continue;
			}
			int colon = line.indexOf(':');
			if (colon < 0) break;                       // first non-key line: species count
			var key = line.substring(0, colon).trim();
			var val = line.substring(colon + 1).trim();
			applyHeaderEntry(settings, key, val);
			i++;
		}

		// ----- species count -----
		while (i < lines.size() && lines.get(i).trim().isEmpty()) i++;
		if (i >= lines.size()) {
			if (true) {
				System.err.println("Settings file ended before species count");
				return settings;
			} else
				throw new IOException("Settings file ended before species count");
		}
		int speciesCount = Integer.parseInt(lines.get(i).trim());
		i++;

		// ----- species names (space-separated, on one line) -----
		while (i < lines.size() && lines.get(i).trim().isEmpty()) i++;
		if (i >= lines.size()) {
			if (true) {
				System.err.println("Settings file ended before species list");
				return settings;
			} else
				throw new IOException("Settings file ended before species list");

		}
		var names = lines.get(i).trim().split("\\s+");
		if (names.length != speciesCount)
			throw new IOException("Species count " + speciesCount + " does not match number of names (" + names.length + ")");
		settings.species.setAll(Arrays.asList(names));
		i++;

		// ----- lineage assignments: "<species> <lineage>" -----
		int idx = 1;
		while (i < lines.size()) {
			var line = lines.get(i).trim();
			i++;
			if (line.isEmpty()) continue;
			var parts = line.split("\\s+", 2);
			if (parts.length < 2) continue;
			settings.lineageAssignments.add(new LineageAssignment(idx++, parts[1], parts[0]));
		}
		return settings;
	}

	private static void applyHeaderEntry(Settings s, String key, String value) {
		switch (key) {
			case Key.MODEL -> s.model = Model.fromCode(Integer.parseInt(value));
			case Key.GAPS -> s.includeAllSites = parseBool01(value);
			case Key.BOOTSTRAP -> s.bootstrapReplicates = Integer.parseInt(value);
			case Key.THETA -> s.theta = Double.parseDouble(value);
			case Key.RATE_PARAM -> s.gammaRate = Double.parseDouble(value);
			case Key.RANDOM_TREE -> s.startingTreeSource = parseBool01(value)
					? StartingTreeSource.GENERATE_RANDOM
					: StartingTreeSource.READ_FROM_FILE;
			case Key.OPT_BL -> s.branchLengthMethod = BranchLengthMethod.fromCode(Integer.parseInt(value));
			case Key.USER_BL -> s.useBranchLengthsFromTree = parseBool01(value);
			case Key.NUM_OPT -> s.branchLengthIterations = Long.parseLong(value);
			case Key.SEED1 -> s.randomSeed1 = Long.parseLong(value);
			case Key.SEED2 -> s.randomSeed2 = Long.parseLong(value);
			case Key.NUM_CAT -> s.gammaCategories = Integer.parseInt(value);
			case Key.TREE_SEARCH -> s.treeSearchMethod = TreeSearchMethod.fromCode(Integer.parseInt(value));
			case Key.NUM_ITER -> s.treeSearchIterations = Long.parseLong(value);
			case Key.MULTI_ITER -> s.multiIter = Integer.parseInt(value);
			case Key.PROB_BOUND -> s.probBound = Double.parseDouble(value);
			case Key.TEST_INCR -> s.testIncr = Integer.parseInt(value);
			case Key.OPT_SLOPE -> s.optSlope = Double.parseDouble(value);
			case Key.BETA -> s.coolingRate = Double.parseDouble(value);
			case Key.VERBOSE -> s.verboseOutput = parseBool01(value);
			default -> { /* ignore unknown keys for forward compatibility */ }
		}
	}

	/**
	 * Writes this instance to a PICL settings file in the canonical format.
	 */
	public void write(Path path) throws IOException {
		try (Writer w = Files.newBufferedWriter(path)) {
			writeTo(w);
		}
	}

	/**
	 * Writes the canonical PICL settings format to any Writer.
	 */
	public void writeTo(Writer w) throws IOException {
		// header — order matches PICL's expected layout
		kv(w, Key.MODEL, Integer.toString(model.code()));
		kv(w, Key.GAPS, bool01(includeAllSites));
		kv(w, Key.BOOTSTRAP, Integer.toString(bootstrapReplicates));
		kv(w, Key.THETA, Double.toString(theta));
		kv(w, Key.RATE_PARAM, Double.toString(gammaRate));
		kv(w, Key.RANDOM_TREE, bool01(startingTreeSource == StartingTreeSource.GENERATE_RANDOM));
		kv(w, Key.OPT_BL, Integer.toString(branchLengthMethod.code()));
		kv(w, Key.USER_BL, bool01(useBranchLengthsFromTree));
		kv(w, Key.NUM_OPT, Long.toString(branchLengthIterations));
		kv(w, Key.SEED1, Long.toString(randomSeed1));
		kv(w, Key.SEED2, Long.toString(randomSeed2));
		kv(w, Key.NUM_CAT, Integer.toString(gammaCategories));
		kv(w, Key.TREE_SEARCH, Integer.toString(treeSearchMethod.code()));
		kv(w, Key.NUM_ITER, Long.toString(treeSearchIterations));
		kv(w, Key.MULTI_ITER, Integer.toString(multiIter));
		kv(w, Key.PROB_BOUND, Double.toString(probBound));
		kv(w, Key.TEST_INCR, Integer.toString(testIncr));
		kv(w, Key.OPT_SLOPE, Double.toString(optSlope));
		kv(w, Key.BETA, Double.toString(coolingRate));
		kv(w, Key.VERBOSE, bool01(verboseOutput));

		// species block — ensure species list is consistent with the assignments
		var speciesToWrite = effectiveSpecies();
		w.write(Integer.toString(speciesToWrite.size()));
		w.write('\n');
		w.write(String.join(" ", speciesToWrite));
		w.write('\n');

		// lineage assignments: "<species> <lineage>"
		for (var la : lineageAssignments) {
			w.write(la.getSpecies() == null ? "" : la.getSpecies());
			w.write(' ');
			w.write(la.getLineage() == null ? "" : la.getLineage());
			w.write('\n');
		}
	}

	/**
	 * Returns the species list, or — if empty — the unique species from the lineage rows.
	 */
	private List<String> effectiveSpecies() {
		if (!species.isEmpty()) return new ArrayList<>(species);
		var ordered = new LinkedHashSet<String>();
		for (var la : lineageAssignments) {
			var sp = la.getSpecies();
			if (sp != null && !sp.isBlank()) ordered.add(sp);
		}
		return new ArrayList<>(ordered);
	}

	/**
	 * Renders the same content write() would produce — used by the Preview button.
	 */
	public String preview() {
		var sw = new StringWriter();
		try {
			writeTo(sw);
		} catch (IOException ex) {
			return "Error: " + ex.getMessage();
		}
		return sw.toString();
	}

	private static void kv(Writer w, String key, String value) throws IOException {
		w.write(key);
		w.write(": ");
		w.write(value);
		w.write('\n');
	}

	private static String bool01(boolean v) {
		return v ? "1" : "0";
	}

	private static boolean parseBool01(String v) {
		return "1".equals(v.trim());
	}

	/**
	 * Validate; returns a list of human-readable problems (empty = OK).
	 */
	public List<String> validate() {
		var problems = new java.util.ArrayList<String>();
		if (alignmentFile == null || alignmentFile.isBlank())
			problems.add("Alignment file is empty.");
		if (theta <= 0) problems.add("θ must be > 0.");
		if (gammaCategories < 1) problems.add("Gamma categories must be ≥ 1.");
		if (branchLengthIterations < 1) problems.add("Branch-length iterations must be ≥ 1.");
		if (treeSearchIterations < 1) problems.add("Tree-search iterations must be ≥ 1.");
		if (treeSearchMethod.usesCoolingRate() && coolingRate <= 0)
			problems.add("Cooling rate β must be > 0 when using simulated annealing.");
		if (bootstrapReplicates < 0) problems.add("Bootstrap replicates cannot be negative.");
		if (startingTreeSource == StartingTreeSource.READ_FROM_FILE
			&& (treeFile == null || treeFile.isBlank()))
			problems.add("Tree file is empty but starting tree is set to read from file.");
		if (!branchLengthMethod.isImplemented())
			problems.add(branchLengthMethod.displayName() + " is not yet implemented in PICL.");
		return problems;
	}
}
