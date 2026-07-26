/*
 * InstallerDownloader.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.OptionalLong;

public class InstallerDownloader {
	private final static HttpClient client;

	static {
		client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
	}

	public static Path download(PlatformInstaller installer, Path targetDirectory) throws Exception {
		return download(installer, targetDirectory, null);
	}

	public static Path download(PlatformInstaller installer, Path targetDirectory, DownloadProgressListener progressListener) throws Exception {
		var uri = URI.create(installer.getInstallerUrl());
		var fileName = Path.of(uri.getPath()).getFileName().toString();
		var target = targetDirectory.resolve(fileName);
		var tmp = target.resolveSibling(target.getFileName() + ".download");

		Files.deleteIfExists(tmp);
		Files.deleteIfExists(target);

		var request = HttpRequest.newBuilder(uri).GET().build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Download failed: HTTP " + response.statusCode() + ", URI: " + response.uri());
		}

		long totalBytes = contentLength(response).orElse(-1L);
		try {
			copyWithProgress(response.body(), tmp, totalBytes, progressListener);
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (UserCanceledException ex) {
			Files.deleteIfExists(tmp);
			Files.deleteIfExists(target);
			throw ex;
		}

		verifyChecksum(target, installer.getSha256());
		return target;
	}

	private static OptionalLong contentLength(HttpResponse<?> response) {
		return response.headers().firstValueAsLong("content-length");
	}

	private static void copyWithProgress(InputStream inputStream, Path target, long totalBytes, DownloadProgressListener progressListener) throws IOException, UserCanceledException {
		try (var in = inputStream; var out = Files.newOutputStream(target)) {
			var buffer = new byte[1024 * 128];
			long totalRead = 0;
			int read;
			if (progressListener != null)
				progressListener.onProgress(0, totalBytes);
			while ((read = in.read(buffer)) >= 0) {
				out.write(buffer, 0, read);
				totalRead += read;
				if (progressListener != null)
					progressListener.onProgress(totalRead, totalBytes);
			}
		}
	}

	private static void verifyChecksum(Path file, String expected) throws Exception {
		if (expected == null || expected.isBlank()) {
			throw new IOException("Manifest is missing sha256 for this installer");
		}
		var actual = sha256(file);
		if (!actual.equalsIgnoreCase(expected.trim())) {
			throw new IOException("Checksum verification failed\nExpected: " + expected + "\nActual: " + actual + "\nFile: " + file + "\nSize: " + Files.size(file) + " bytes");
		}
	}

	private static String sha256(Path file) throws Exception {
		var digest = MessageDigest.getInstance("SHA-256");
		try (var in = Files.newInputStream(file)) {
			var buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
		}

		return HexFormat.of().formatHex(digest.digest());
	}
}
