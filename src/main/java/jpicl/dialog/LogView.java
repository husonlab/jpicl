/*
 * LogView.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.dialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogView extends BorderPane {
	private final ListView<String> listView = new ListView<>();
	private final ObservableList<String> lines = FXCollections.observableArrayList();

	private final BooleanProperty empty = new SimpleBooleanProperty(this, "empty", true);

	private final ConcurrentLinkedQueue<String> pendingLines = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean updateScheduled = new AtomicBoolean(false);

	private final int maxLines;

	public LogView() {
		this(20_000);
		empty.bind(Bindings.size(lines).lessThanOrEqualTo(1));
		clear();
	}

	public LogView(int maxLines) {
		this.maxLines = maxLines;

		listView.getStyleClass().add("log-view");

		listView.setItems(lines);
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView.setFocusTraversable(true);

		listView.setCellFactory(v -> new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item);
			}
		});

		var copyItem = new MenuItem("Copy selected lines");
		copyItem.setOnAction(e -> copySelectedLines());

		var copyAllItem = new MenuItem("Copy all");
		copyAllItem.setOnAction(e -> copyAllLines());

		var clearItem = new MenuItem("Clear");
		clearItem.setOnAction(e -> clear());

		listView.setContextMenu(new ContextMenu(copyItem, copyAllItem, new SeparatorMenuItem(), clearItem));

		listView.setOnKeyPressed(e -> {
			if (e.isShortcutDown()) {
				switch (e.getCode()) {
					case C -> {
						copySelectedLines();
						e.consume();
					}
					case A -> {
						listView.getSelectionModel().selectAll();
						e.consume();
					}
				}
			}
		});
		setCenter(listView);
	}

	public void appendLine(String line) {
		pendingLines.add(line == null ? "" : line);
		scheduleFlush();
	}

	public void appendText(String text) {
		if (text == null || text.isEmpty())
			return;

		String[] split = text.split("\\R", -1);
		for (String line : split) {
			if (!line.isEmpty())
				pendingLines.add(line);
		}

		scheduleFlush();
	}

	public void clear() {
		pendingLines.clear();

		if (Platform.isFxApplicationThread()) {
			lines.clear();
		} else {
			Platform.runLater(lines::clear);
		}
	}

	public ListView<String> getListView() {
		return listView;
	}

	private void scheduleFlush() {
		if (updateScheduled.compareAndSet(false, true)) {
			Platform.runLater(this::flushPendingLines);
		}
	}

	private void flushPendingLines() {
		updateScheduled.set(false);

		List<String> batch = new ArrayList<>(1000);

		String line;
		while ((line = pendingLines.poll()) != null && batch.size() < 1000) {
			batch.add(line);
		}

		if (!batch.isEmpty()) {
			lines.addAll(batch);

			int overflow = lines.size() - maxLines;
			if (overflow > 0) {
				lines.remove(0, overflow);
			}

			listView.scrollTo(lines.size() - 1);
		}

		if (!pendingLines.isEmpty()) {
			scheduleFlush();
		}
	}

	public boolean copySelectedLines() {
		var selected = listView.getSelectionModel().getSelectedItems();

		if (selected == null || selected.isEmpty()) {
			return false;
		} else {
			copyToClipboard(String.join(System.lineSeparator(), selected));
			return true;
		}
	}

	public void copyAllLines() {
		copyToClipboard(String.join(System.lineSeparator(), lines));
	}

	private static void copyToClipboard(String text) {
		var content = new ClipboardContent();
		content.putString(text);
		Clipboard.getSystemClipboard().setContent(content);
	}

	public boolean isEmpty() {
		return empty.get();
	}

	public ReadOnlyBooleanProperty emptyProperty() {
		return empty;
	}
}