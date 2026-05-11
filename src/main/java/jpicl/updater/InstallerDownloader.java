/*
 * DownloadInstaller.java Copyright (C) 2026 Daniel H. Huson
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public class InstallerDownloader {
	private final static HttpClient client;

	static {
		client = HttpClient.newHttpClient();
	}
	public static Path download(PlatformInstaller installer, Path targetDirectory) throws Exception {
		var uri = URI.create(installer.getInstallerUrl());
		var fileName = Path.of(uri.getPath()).getFileName().toString();
		var target = targetDirectory.resolve(fileName);
		client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofFile(target));
		verifyChecksum(target, installer.getSha256());
		return target;
	}

	private static void verifyChecksum(Path file, String expected) throws Exception {
		if (expected == null || expected.isBlank()) {
			throw new IOException("Manifest is missing sha256 for this installer");
		}
		var actual = sha256(file);
		if (!actual.equalsIgnoreCase(expected)) {
			throw new IOException("Checksum verification failed\nExpected: " + expected + "\nActual: " + actual);
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