/*
 * VersionComparator.java Copyright (C) 2026 Daniel H. Huson
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

public class VersionComparator {

	public static boolean isNewer(String a, String b) {
		return compare(a, b) > 0;
	}

	public static int compare(String a, String b) {
		var aa = normalize(a).split("\\.");
		var bb = normalize(b).split("\\.");

		var n = Math.max(aa.length, bb.length);

		for (var i = 0; i < n; i++) {
			var ai = i < aa.length ? Integer.parseInt(aa[i]) : 0;
			var bi = i < bb.length ? Integer.parseInt(bb[i]) : 0;

			if (ai != bi)
				return Integer.compare(ai, bi);
		}
		return 0;
	}

	private static String normalize(String v) {
		if (v == null || v.isBlank() || "dev".equals(v)) return "0";
		int cut = -1;
		for (int i = 0; i < v.length(); i++) {
			char c = v.charAt(i);
			if (c != '.' && !Character.isDigit(c)) {
				cut = i;
				break;
			}
		}
		return cut < 0 ? v : v.substring(0, cut);
	}
}