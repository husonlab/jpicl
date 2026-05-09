/*
 * DrawPhylogram.java Copyright (C) 2026 Daniel H. Huson
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

package jpicl.draw;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import jpicl.util.TreeNode;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleFunction;

/**
 * draws a phylogram
 * Daniel Huson, 4.2026
 */
public class DrawPhylogram {
	public static Group draw(TreeNode root, double width, double height) {
		var counter = new LongAdder();

		root.postOrderTraversal(v -> {
			if (v.isLeaf()) {
				counter.increment();
				v.setY(counter.intValue());
			} else {
				v.setY(v.getChildren().stream().mapToDouble(TreeNode::getY).average().orElse(0.0));
			}
		});

		root.preOrderTraversal(v -> {
			if (v == root) {
				v.setX(0.0);
			} else {
				v.setX(v.getParent().getX() + v.getWeight());
			}
		});

		// map into bounds:
		var transformX = setupTransformX(root, width);
		var transformY = setupTransformY(root, height);

		var edgeLabelGroup = new Group();
		var edgeGroup = new Group();
		var nodeGroup = new Group();
		var nodeLabelGroup = new Group();

		root.preOrderTraversal(v -> {
			var vX = transformX.apply(v.getX());
			var vY = transformY.apply(v.getY());
			var shape = new Circle(3, vX, vY);
			nodeGroup.getChildren().add(shape);
			if (v.getLabel() != null && !v.getLabel().isBlank()) {
				var label = new Label(v.getLabel());
				label.setLayoutX(vX + shape.getRadius() + 4);
				label.setLayoutY(vY - label.getFont().getSize() / 2);
				nodeLabelGroup.getChildren().add(label);
			}
			if (!v.isLeaf() && v.getParent() != null && v.getConfidence() >= 0) {
				var label = new Label("%.1f".formatted(v.getConfidence()));
				label.setLayoutX(vX + 3);
				label.setLayoutY(vY + 3);
				edgeLabelGroup.getChildren().add(label);
			}
			for (var w : v.getChildren()) {
				var wX = transformX.apply(w.getX());
				var wY = transformY.apply(w.getY());
				var path = new Path(new MoveTo(vX, vY), new LineTo(vX, wY), new LineTo(wX, wY));
				edgeGroup.getChildren().add(path);
			}
		});

		return new Group(edgeLabelGroup, edgeGroup, nodeGroup, nodeLabelGroup);
	}

	private static DoubleFunction<Double> setupTransformX(TreeNode root, double width) {
		var minMax = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};

		root.preOrderTraversal(v -> {
			minMax[0] = Math.min(minMax[0], v.getX());
			minMax[1] = Math.max(minMax[1], v.getX());
		});
		return x -> (x - minMax[0]) / (minMax[1] - minMax[0]) * width;
	}

	private static DoubleFunction<Double> setupTransformY(TreeNode root, double height) {
		var minMax = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};

		root.preOrderTraversal(v -> {
			minMax[0] = Math.min(minMax[0], v.getY());
			minMax[1] = Math.max(minMax[1], v.getY());
		});
		return y -> (y - minMax[0]) / (minMax[1] - minMax[0]) * height;
	}
}
