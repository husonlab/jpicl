/*
 * TreeNode.java Copyright (C) 2026 Daniel H. Huson
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TreeNode {
	private String label;

	private double x;
	private double y;

	private TreeNode parent;
	private final List<TreeNode> children = new ArrayList<>();

	private Double weight;
	private Double confidence = -1.0;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public TreeNode getParent() {
		return parent;
	}

	public List<TreeNode> getChildren() {
		return children;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		this.confidence = confidence;
	}

	public void addChild(TreeNode child) {
		children.add(child);
		child.parent = this;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private String toString(int depth) {
		StringBuilder sb = new StringBuilder();

		sb.append("  ".repeat(depth));

		sb.append(label != null ? label : "*");

		if (weight != null)
			sb.append(" w=").append(weight);

		if (confidence != null)
			sb.append(" c=").append(confidence);

		sb.append("\n");

		for (TreeNode child : children) {
			sb.append(child.toString(depth + 1));
		}

		return sb.toString();
	}

	public void preOrderTraversal(Consumer<TreeNode> consumer) {
		consumer.accept(this);
		for (var v : getChildren()) {
			v.preOrderTraversal(consumer);
		}
	}

	public void postOrderTraversal(Consumer<TreeNode> consumer) {
		for (var v : getChildren()) {
			v.postOrderTraversal(consumer);
		}
		consumer.accept(this);
	}
}
