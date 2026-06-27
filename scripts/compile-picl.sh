#!/bin/zsh
set -e

SCRIPT_DIR="${0:A:h}"

case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)   PLATFORM_KEY="macos-aarch64"; OS="mac"   ;;
    Darwin-x86_64)  PLATFORM_KEY="macos-x86_64";  OS="mac"   ;;
    Linux-x86_64)   PLATFORM_KEY="linux-x86_64";  OS="linux" ;;
    Linux-aarch64)  PLATFORM_KEY="linux-aarch64"; OS="linux" ;;
    *) echo "Unknown platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

DEST_DIR="$SCRIPT_DIR/../src/main/resources/native/$PLATFORM_KEY"
mkdir -p "$DEST_DIR"

cd "$SCRIPT_DIR/../native/picl/src"

# PICL is a C "unity build": main.c #includes all the other .c files. It now
# also calls besselK_c(), which is DEFINED in a small C++ Bessel wrapper. The
# old one-line build (gcc main.c -lm) never compiled that wrapper, hence:
#     Undefined symbols ... "_besselK_c"  ->  ld: symbol(s) not found
#
# Fix: compile the wrapper too and link everything with the C++ compiler (so
# the C++ runtime is pulled in). Two wrappers ship; we pick the right one:
#   * macOS  -> bessel_wrapper_macos.cpp  (boost::math::cyl_bessel_k)
#   * Linux  -> bessel_wrapper.cpp        (std::cyl_bessel_k from libstdc++)
# (Equivalently, you can just run `make` here — Laura's Makefile does the same.)

rm -f main.o bessel_wrapper.o picl

if [[ "$OS" == "mac" ]]; then
    # macOS libc++ does NOT implement C++17 std::cyl_bessel_k, so the macOS
    # wrapper uses Boost.Math, which is HEADER-ONLY: we only need its include
    # path — there is nothing to link, and the binary gains no Boost runtime
    # dependency.
    BOOST_PREFIX="$(brew --prefix boost 2>/dev/null || true)"
    if [[ -z "$BOOST_PREFIX" || ! -f "$BOOST_PREFIX/include/boost/math/special_functions/bessel.hpp" ]]; then
        echo "Boost headers not found. Install them once with:" >&2
        echo "    brew install boost" >&2
        exit 1
    fi
    echo "Using Boost headers at: $BOOST_PREFIX/include"
    clang   -O2 -c main.c -o main.o
    clang++ -O2 -std=c++17 -I"$BOOST_PREFIX/include" -c bessel_wrapper_macos.cpp -o bessel_wrapper.o
    clang++ -O2 main.o bessel_wrapper.o -lm -o picl
else
    # Linux: libstdc++ provides std::cyl_bessel_k, so no Boost is needed.
    # Static-link the C++ runtime so the local binary is self-contained.
    gcc -O2 -c main.c -o main.o
    g++ -O2 -std=c++17 -c bessel_wrapper.cpp -o bessel_wrapper.o
    g++ -static-libgcc -static-libstdc++ main.o bessel_wrapper.o -lm -o picl
fi

rm -f main.o bessel_wrapper.o
mv picl "$DEST_DIR/picl"

echo "Built picl ($PLATFORM_KEY) → $DEST_DIR/picl"