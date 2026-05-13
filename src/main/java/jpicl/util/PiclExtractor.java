package jpicl.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Locates the platform-appropriate PICL binary that ships inside the JAR
 * under {@code /native/&lt;platform-key&gt;/picl[.exe]}, extracts it to a
 * cache directory under the user's home, marks it executable, and
 * returns its absolute {@link Path}.
 *
 * <p>The cache directory uses a hash of the resource bytes as its key,
 * so a recompiled-and-bundled binary automatically invalidates older
 * caches without needing a manual version bump. Subsequent calls for
 * an unchanged binary find the existing file and return immediately.
 *
 * <h2>Platform keys</h2>
 * <ul>
 *   <li>macOS Apple Silicon → {@code macos-aarch64}</li>
 *   <li>macOS Intel         → {@code macos-x86_64}</li>
 *   <li>Linux 64-bit        → {@code linux-x86_64}</li>
 *   <li>Windows 64-bit      → {@code windows-x86_64}</li>
 * </ul>
 *
 * <p>If the JAR doesn't bundle a binary for the current platform, the
 * extractor throws {@link UnsupportedOperationException} — callers
 * should fall back to a user-configured path.
 */
public final class PiclExtractor {

	private static final String CACHE_ROOT = ".cache/jpicl/picl";

	private PiclExtractor() {
	}

	/**
	 * Returns a path to a runnable PICL binary, extracting from the
	 * JAR on first use and caching for subsequent calls.
	 */
	public static Path resolveExecutable() {
		var key = platformKey();
		var name = key.startsWith("windows") ? "picl.exe" : "picl";

		if (System.getProperty("jpackage.app-path") != null) {
			var appPath = Path.of(System.getProperty("jpackage.app-path"));
			if (key.startsWith("macos")) {
				return appPath.getParent().getParent().resolve("Resources").resolve("native").resolve("picl");
			} else {
				String exe = key.startsWith("windows") ? "picl.exe" : "picl";
				return appPath.getParent().resolve("native").resolve(exe);
			}
		}

		var dev = Path.of("packaging", "native", key, name);
		if (!Files.exists(dev))
			System.err.println("no file");
		if (Files.isExecutable(dev)) {
			return dev;
		}
		throw new IllegalStateException("PICL executable not found or not executable: " + dev);
	}

	/**
	 * Computes the platform key matching the JAR resource layout.
	 */
	public static String platformKey() {
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();

		String archKey;
		if (arch.contains("aarch64") || arch.contains("arm64") || arch.equals("arm")) {
			archKey = "aarch64";
		} else {
			archKey = "x86_64";   // covers x86_64 / amd64
		}

		if (os.contains("mac") || os.contains("darwin")) return "macos-" + archKey;
		if (os.contains("linux")) return "linux-" + archKey;
		if (os.contains("windows")) return "windows-" + archKey;
		throw new UnsupportedOperationException(
				"Unsupported platform: os=" + os + ", arch=" + arch);
	}

	private static String sha256Hex(byte[] bytes) {
		try {
			var md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(bytes));
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 unavailable", e);
		}
	}
}
