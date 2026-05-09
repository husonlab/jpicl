# How to ship a new PICL version in jpicl

When Laura pushes a new major version of PICL, here's the recipe to roll
it into a published jpicl JAR with binaries for all four platforms.

The whole loop takes ~10 minutes of your time plus ~4 minutes of CI.

---

## 1. Bump the submodule and sanity-check on your M4

```sh
./scripts/bump-to-lauras-latest-picl.sh    # records the new commit in jpicl
./scripts/compile-picl.sh                  # compiles + drops binary into resources/
```

Run jpicl from IntelliJ or `mvn package && java -jar target/jpicl-*.jar`,
load a small example settings file, click **Run PICL**, and confirm:

- The Log tab fills with output and finishes with a clean exit code.
- The Trees tab populates from the `.treeinfo` file.
- The auto-derived files (`.settings`, `.values`, `.log`, `.bootstrap`)
  appear next to the alignment and output tree.

If anything looks off, that's almost always the C side — Laura's new
release likely changed file format, argv layout, or output filenames.
Fix that *before* triggering CI; otherwise you'll just publish broken
binaries on three other platforms too.

> **Most common breakages:** new positional argument added (update the
> `ProcessBuilder` argv in `DialogController.onRunPicl`); new key in
> the settings header (update `Settings.Key` and `applyHeaderEntry`);
> new output file (add a derive helper + label).

## 2. Trigger the binaries build on GitHub

Push your `bump` commit. The workflow at `.github/workflows/build-picl.yml`
fires automatically on any change under `native/picl/**`.

Or trigger manually: GitHub → **Actions** → **Build picl binaries** →
**Run workflow** → main branch.

You'll see four parallel matrix jobs (`macos-x86_64`, `macos-aarch64`,
`linux-x86_64`, `windows-x86_64`) followed by a single `bundle` job.

## 3. Download the combined binaries artifact

When the run goes green, scroll to **Artifacts** at the bottom of the
run page. Download **`picl-binaries-all`**. It's a zip whose top-level
layout is:

```
macos-aarch64/picl
macos-x86_64/picl
linux-x86_64/picl
windows-x86_64/picl.exe
```

This matches `src/main/resources/native/` exactly.

## 4. Drop binaries into the resources tree

```sh
cd ~/IdeaProjects/apps/jpicl
unzip -o ~/Downloads/picl-binaries-all.zip -d src/main/resources/native/
```

Verify all four binaries now exist under `src/main/resources/native/`.

## 5. Build and smoke-test the fat JAR

```sh
mvn clean package
unzip -l target/jpicl-*.jar | grep native/      # should list all 4 binaries
java -jar target/jpicl-*.jar                    # quick run from your M4
```

The M4 binary in the JAR was just built fresh by CI on a different
machine, so this step also validates that the per-platform build is
byte-compatible with the local one (in practice it's bit-identical for
deterministic source like PICL).

## 6. Commit and tag the release

```sh
git add src/main/resources/native/
git commit -m "Update bundled picl binaries to PICL <new-commit-sha>"
git tag -a v<jpicl-version> -m "jpicl <jpicl-version> with PICL <new-commit-sha>"
git push origin main --tags
```

The tag is what humans (and you, six months from now) will use to
reference this release. Include both the jpicl version and the PICL
commit short-hash in the tag message — that single line saves a *lot*
of git archaeology later.

---

## Things that go wrong, and what to do

- **CI build fails on one platform.** Open the failed job's logs.
    - Compile error → PICL needs a tweak (more common on Windows than
      elsewhere, e.g. POSIX-isms that MinGW doesn't have).
    - "macOS image deprecated" → bump `macos-13` / `macos-14` to the next
      available version in the matrix.
    - glibc complaint on Linux → swap `ubuntu-22.04` for an older runner
      or build inside a `manylinux` container for that one matrix entry.

- **`picl-binaries-all` artifact missing.** The `bundle` job needs every
  matrix entry to succeed. Re-run failed jobs from the GitHub UI rather
  than rerunning the whole workflow.

- **macOS users get "picl can't be opened, it can't be checked for
  malicious software".** Gatekeeper. Either tell them to right-click →
  Open the first time, or sign + notarize in install4j (you have the
  Developer ID already). The `xattr -d com.apple.quarantine` workaround
  could also be added to `PiclExtractor` if you'd rather not notarize.

- **`PiclExtractor` complains "No PICL binary bundled for platform X".**
  The user is on a platform we don't ship for (32-bit Linux, BSD, etc.).
  Add it to the CI matrix if you need to support it.

- **Cache is stale after a binary change.** It shouldn't be —
  `PiclExtractor` keys the cache by the SHA-256 of the resource bytes,
  so a new binary lands in a new cache directory. If you ever need to
  nuke caches manually: `rm -rf ~/.cache/jpicl/picl/`.

---

## File reference

| File                                          | Purpose                                                                       |
|-----------------------------------------------|-------------------------------------------------------------------------------|
| `scripts/bump-to-lauras-latest-picl.sh`       | Records the latest PICL commit in jpicl                                       |
| `scripts/compile-picl.sh`                     | Compiles PICL locally + copies binary into `src/main/resources/native/<key>/` |
| `.github/workflows/build-picl.yml`            | CI matrix that builds picl on all four platforms                              |
| `src/main/resources/native/<key>/picl[.exe]`  | Where bundled binaries live (read by `PiclExtractor`)                         |
| `src/main/java/jpicl/util/PiclExtractor.java` | Runtime extraction + caching of the right binary for the running OS           |
