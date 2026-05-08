package jpicl.util;

import java.io.IOException;
import java.io.InputStream;
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
	public static Path resolveExecutable() throws IOException {
		var key = platformKey();
		var name = key.startsWith("windows") ? "picl.exe" : "picl";
		var resource = "/native/" + key + "/" + name;

		try (InputStream in = PiclExtractor.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new UnsupportedOperationException(
						"No PICL binary bundled for platform " + key
						+ " (expected resource " + resource + ").");
			}
			byte[] bytes = in.readAllBytes();

			// 16 hex chars (= 64 bits) is plenty to disambiguate caches.
			String hash = sha256Hex(bytes).substring(0, 16);
			Path cacheDir = Path.of(System.getProperty("user.home"), CACHE_ROOT, hash, key);
			Path target = cacheDir.resolve(name);

			if (Files.isExecutable(target)) return target;

			Files.createDirectories(cacheDir);
			Files.write(target, bytes);
			if (!key.startsWith("windows")) {
				// rwxr-xr-x — anyone who can read the file can run it.
				target.toFile().setExecutable(true, false);
			}
			return target;
		}
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
