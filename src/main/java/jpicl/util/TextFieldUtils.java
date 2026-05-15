/*
 * TextFieldUtils.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Utilities for restricting TextField input to numeric values within a range.
 * Filters keystrokes in real time and clamps to [min, max] on focus loss.
 */
public class TextFieldUtils {
	public static void setDouble(TextField textField) {
		setDouble(Double.MIN_VALUE, Double.MAX_VALUE, textField);
	}

	/**
	 * Restricts {@code textField} to double values in [min, max].
	 * Invalid keystrokes are rejected; on focus loss the value is clamped.
	 */
	public static void setDouble(double min, double max, TextField textField) {
		textField.setTextFormatter(new TextFormatter<>(change -> {
			String s = change.getControlNewText();
			if (s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.")) return change;
			try {
				Double.parseDouble(s);
				return change;
			} catch (NumberFormatException e) {
				return null;
			}
		}));

		textField.focusedProperty().addListener((obs, was, isFocused) -> {
			if (!isFocused) {
				double v;
				try {
					v = Double.parseDouble(textField.getText());
				} catch (NumberFormatException e) {
					v = min;
				}
				if (v < min) v = min;
				else if (v > max) v = max;
				textField.setText(Double.toString(v));
			}
		});
	}

	public static void setInteger(TextField textField) {
		setInteger(Integer.MIN_VALUE, Integer.MAX_VALUE, textField);
	}

	/**
	 * Restricts {@code textField} to integer values in [min, max].
	 * Invalid keystrokes are rejected; on focus loss the value is clamped.
	 */
	public static void setInteger(int min, int max, TextField textField) {
		textField.setTextFormatter(new TextFormatter<>(change -> {
			String s = change.getControlNewText();
			if (s.isEmpty() || s.equals("-")) return change;
			try {
				Integer.parseInt(s);
				return change;
			} catch (NumberFormatException e) {
				return null;
			}
		}));

		textField.focusedProperty().addListener((obs, was, isFocused) -> {
			if (!isFocused) {
				int v;
				try {
					v = Integer.parseInt(textField.getText());
				} catch (NumberFormatException e) {
					v = min;
				}
				if (v < min) v = min;
				else if (v > max) v = max;
				textField.setText(Integer.toString(v));
			}
		});
	}

	/**
	 * Convenience: read current double value, falling back to {@code defaultValue} on parse error.
	 */
	public static double getDouble(TextField textField, double defaultValue) {
		try {
			return Double.parseDouble(textField.getText());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Convenience: read current int value, falling back to {@code defaultValue} on parse error.
	 */
	public static int getInteger(TextField textField, int defaultValue) {
		try {
			return Integer.parseInt(textField.getText());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}