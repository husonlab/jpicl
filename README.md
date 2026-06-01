# JPICL

A cross-platform desktop interface for [PICL](https://github.com/lkubatko/PICL),
Laura Kubatko's C library for phylogenetic inference with composite
likelihood under the multispecies coalescent.

JPICL gives PICL a JavaFX user interface, a tree-visualisation panel,
sequence-format conversion (FASTA in addition to Phylip), and an
in-app updater. The PICL binary is bundled inside the application JAR
for macOS (Intel and Apple Silicon), Linux (x86_64), and Windows
(x86_64) — no separate compilation step required for end users.

---

## Features

- Edit a PICL run interactively: model, branch-length method,
  tree-search method, bootstrap settings, random seeds, and the full
  lineage → species mapping.
- Load and save settings files in PICL's native format.
- Read alignments in FASTA or Phylip — FASTA is auto-converted to a
  temporary Phylip file before being handed to PICL.
- One-click run: launches PICL as a subprocess, streams stdout/stderr
  to a Log tab, and writes a `.log` file beside the outputs.
- Output panes: a textual view of the multi-tree `.trees` file and a
  graphical phylogram of the final `.tre` file.
- Non-destructive collision handling: if outputs from a previous run
  already exist, JPICL asks before bumping to `data-2.tre`,
  `data-3.tre`, etc. — your earlier results are never overwritten.
- Per-platform installers, with an in-app **View → Check for
  Updates…** that fetches a manifest, compares versions, downloads
  the right installer for your OS, and verifies its SHA-256 before
  launching it.

## Installation

Download the latest installer for your platform from the
[releases page](https://github.com/husonlab/jpicl/releases).


After install, launch JPICL from your application launcher.

## Quick start

1. **Open an alignment.** File → Open the alignment file (FASTA or
   Phylip).
2. **Define lineages.** Use **Lineages from data** to populate the
   table from the alignment headers, then assign each lineage to a
   species (you can also import a tab-separated mapping or
   auto-detect species by prefix).
3. **Pick a model and search settings** — defaults are sensible for
   most analyses.
4. **Run PICL.** The Log tab fills with progress; on success the
   Output tab shows the annotated tree-info file and the Tree tab
   shows the inferred phylogram.
5. **Save the run.** All output files (`.tre`, `.trees`, `.log`,
   `.bootstrap`, `.values`, `.settings`) sit beside your alignment
   file and share the same basename.

## Building from source

Requirements: JDK 21+ and Maven 3.9+.

```sh
git clone --recurse-submodules https://github.com/your-org/jpicl.git
cd jpicl
mvn package
java -jar target/jpicl-<version>.jar
```

The `--recurse-submodules` flag is important — PICL itself lives at
`native/picl` as a submodule pointing at upstream. If you forget it,
run `git submodule update --init --recursive` from the jpicl root.

To compile a fresh PICL binary for your local platform and drop it
into the resources tree (useful when iterating on Laura's code
without a CI round-trip):

```sh
./scripts/compile-picl.sh
```

To bump the bundled PICL submodule to upstream's latest commit and
record the new pointer in a jpicl commit:

```sh
./scripts/bump-to-lauras-latest-picl.sh
```

## Project layout

```
src/main/java/jpicl/
├── dialog/         JavaFX controller + presenter for the main window
├── draw/           Phylogram rendering (DrawPhylogram)
├── main/           App entry point + Version constants
├── tools/          Standalone CLI utilities (e.g. FastAMerger)
├── util/           Alignment parsers, Newick I/O, output-file
│                   bookkeeping, PICL binary extractor
└── window/         Top-level Stage management

native/picl/        PICL C source (git submodule → lkubatko/PICL)
scripts/            Helper scripts (compile, bump, etc.)
```


## Credits

PICL is the work of [Laura Kubatko](https://github.com/lkubatko) and
collaborators. JPICL provides only the user-interface layer; all
phylogenetic inference is performed by PICL itself.

JPICL is developed by Daniel H. Huson at the University of Tübingen.

## Citation

If you use JPICL or PICL in published work, please cite the PICL
manuscript (see the upstream PICL repository for the current
citation), and optionally reference this interface as:

> TBA

## License

GNU General Public License v3.0. See the headers of individual source
files and the [GPL-3.0 text](https://www.gnu.org/licenses/gpl-3.0.html).
