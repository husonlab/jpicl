name: Build picl binaries

on:
  push:
    paths:
      - 'native/picl/**'
      - '.github/workflows/build-picl.yml'
  workflow_dispatch:

jobs:
  build:
    name: Build (${{ matrix.target }})
    runs-on: ${{ matrix.runner }}
    strategy:
      fail-fast: false
      matrix:
        include:
          # Both macOS slices are built on the Apple Silicon runner.
          # x86_64 is produced by clang cross-compile (-arch x86_64).
          - target: macos-aarch64
            runner: macos-latest
            arch_flag: '-arch arm64'
            binary: picl
          - target: macos-x86_64
            runner: macos-latest
            arch_flag: '-arch x86_64'
            binary: picl
          - target: linux-x86_64
            runner: ubuntu-22.04
            arch_flag: ''
            binary: picl
          - target: windows-x86_64
            runner: windows-2025
            arch_flag: ''
            binary: picl.exe

    steps:
      - name: Check out jpicl (with submodules)
        uses: actions/checkout@v5
        with:
          submodules: recursive

      # ---------- macOS (both arches built on macos-latest) ----------
      - name: Build picl (macOS)
        if: startsWith(matrix.target, 'macos-')
        shell: bash
        run: |
          cd native/picl/src
          clang -O2 ${{ matrix.arch_flag }} -mmacosx-version-min=11.0 \
                main.c -lm -o ${{ matrix.binary }}
          file ${{ matrix.binary }}

      # ---------- Linux ----------
      - name: Build picl (Linux)
        if: matrix.target == 'linux-x86_64'
        shell: bash
        run: |
          cd native/picl/src
          gcc -O2 main.c -lm -o ${{ matrix.binary }}
          file ${{ matrix.binary }}

      # ---------- Windows (MinGW) ----------
      - name: Set up MinGW
        if: matrix.target == 'windows-x86_64'
        uses: msys2/setup-msys2@v2
        with:
          msystem: MINGW64
          install: >-
            mingw-w64-x86_64-gcc
            mingw-w64-x86_64-make

      - name: Build picl (Windows)
        if: matrix.target == 'windows-x86_64'
        shell: 'msys2 {0}'
        run: |
          cd native/picl/src
          gcc -O2 main.c -lm -o ${{ matrix.binary }}
          file ${{ matrix.binary }}

      # ---------- Upload per-platform artifact ----------
      - name: Upload binary
        uses: actions/upload-artifact@v7
        with:
          name: picl-${{ matrix.target }}
          path: native/picl/src/${{ matrix.binary }}
          if-no-files-found: error
          retention-days: 7

  bundle:
    name: Bundle all binaries
    needs: build
    runs-on: ubuntu-22.04
    steps:
      - name: Download all per-platform artifacts
        uses: actions/download-artifact@v7
        with:
          path: staging

      - name: Reorganize into resource layout
        shell: bash
        run: |
          mkdir -p out/macos-aarch64 out/macos-x86_64 out/linux-x86_64 out/windows-x86_64
          cp staging/picl-macos-aarch64/picl     out/macos-aarch64/picl
          cp staging/picl-macos-x86_64/picl      out/macos-x86_64/picl
          cp staging/picl-linux-x86_64/picl      out/linux-x86_64/picl
          cp staging/picl-windows-x86_64/picl.exe out/windows-x86_64/picl.exe
          chmod +x out/macos-aarch64/picl out/macos-x86_64/picl out/linux-x86_64/picl
          ls -lR out

      - name: Upload combined bundle
        uses: actions/upload-artifact@v7
        with:
          name: picl-binaries-all
          path: out/
          if-no-files-found: error
          retention-days: 30
