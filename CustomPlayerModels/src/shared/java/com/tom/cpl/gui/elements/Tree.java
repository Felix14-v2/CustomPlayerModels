package com.tom.cpl.gui.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.tom.cpl.gui.Frame;
import com.tom.cpl.gui.MouseEvent;
import com.tom.cpl.gui.elements.Tree.TreeHandler.TreeElement;
import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;

public class Tree<T> extends GuiElement {
	private Map<Integer, TreeElement<T>> map = new HashMap<>();
	private TreeHandler<T> handler;
	private Frame frame;
	private Consumer<Vec2i> sizeUpdate;

	public Tree(Frame gui, TreeModel<T> model) {
		super(gui.getGui());
		this.frame = gui;
		handler = new TreeHandler<>(model);
	}

	public Tree(Frame gui, TreeHandler<T> handler) {
		super(gui.getGui());
		this.frame = gui;
		this.handler = handler;
	}

	@Override
	public void mouseClick(MouseEvent evt) {
		if(evt.isConsumed())return;
		if(bounds.isInBounds(evt.x, evt.y)) {
			int yp = (evt.y - bounds.y) / 10;
			TreeElement<T> elem = map.get(yp);
			if(elem != null) {
				if(evt.btn == 0 && evt.x < 5 + elem.depth * 5 && elem != handler.root && !elem.children.isEmpty()) {
					elem.showChildren = !elem.showChildren;
					if(sizeUpdate != null)sizeUpdate.accept(getSize());
				} else {
					handler.model.onClick(evt, elem.value);
				}
			} else {
				handler.model.onClick(evt, null);
			}
			handler.model.treeUpdated();
			evt.consume();
		}
	}


	public Vec2i getSize() {
		int[] s = new int[2];
		walk(s, handler.root, 0);
		return new Vec2i(s[0], s[1] * 10);
	}

	private void walk(int[] s, TreeElement<T> e, int layer) {
		s[1]++;
		s[0] = Math.max(s[0], layer * 5 + gui.textWidth(e.display) + 5);
		if(e.showChildren) {
			for (TreeElement<T> i : e.children) {
				walk(s, i, layer + 1);
			}
		}
	}

	@Override
	public void draw(MouseEvent event, float partialTicks) {
		map.clear();
		drawTree(event, 0, new int[1], handler.root);
	}

	private void drawTree(MouseEvent event, int x, int[] y, TreeElement<T> e) {
		int yp = y[0]++;
		int textColor = gui.getColors().button_text_color;
		int bg = e.value == null ? 0 : handler.model.bgColor(e.value);
		if(bg != 0) {
			gui.drawBox(x * 5, yp * 10, bounds.w, 10, bg);
		}
		int txtc = e.value == null ? 0 : handler.model.textColor(e.value);
		if(txtc != 0) {
			textColor = txtc;
		}
		if (event.isHovered(new Box(bounds.x, yp * 10, bounds.w, 10))) {
			textColor = gui.getColors().button_text_hover;
			Tooltip tt = handler.model.getTooltip(e.value);
			if(tt != null)tt.set();
		}
		map.put(yp, e);
		gui.drawText(x * 5 + 3, yp * 10, e.display, textColor);
		e.depth = x;
		if(e.showChildren) {
			if(e != handler.root && !e.children.isEmpty())gui.drawTexture(x * 5 - 5, yp * 10, 8, 8, 24, 8, "editor");
			for (TreeElement<T> i : e.children) {
				drawTree(event, x + 1, y, i);
			}
		} else {
			if(e != handler.root && !e.children.isEmpty())gui.drawTexture(x * 5 - 5, yp * 10, 8, 8, 24, 0, "editor");
		}
	}

	public void updateTree() {
		if(sizeUpdate != null)sizeUpdate.accept(getSize());
	}

	public void setSizeUpdate(Consumer<Vec2i> sizeUpdate) {
		this.sizeUpdate = sizeUpdate;
	}

	public static abstract class TreeModel<T> {
		protected abstract int textColor(T val);
		protected abstract void getElements(T parent, Consumer<T> c);
		protected abstract int bgColor(T val);
		protected abstract void treeUpdated();
		protected abstract void onClick(MouseEvent evt, T elem);
		protected abstract String getName(T elem);
		protected abstract Tooltip getTooltip(T elem);
		protected abstract void refresh(T elem);
	}

	public static class TreeHandler<T> {
		private TreeModel<T> model;
		private TreeElement<T> root;

		public TreeHandler(TreeModel<T> model) {
			this.root = new TreeElement<>();
			this.root.display = "Root";
			this.model = model;
		}

		public static class TreeElement<T> {
			private String display;
			private T value;
			private List<TreeElement<T>> children = new ArrayList<>();
			private boolean showChildren;
			private int depth;
		}

		public void update() {
			List<TreeElement<T>> old = new ArrayList<>(root.children);
			root.children.clear();
			root.showChildren = true;
			model.getElements(null, e -> {
				TreeElement<T> t = new TreeElement<>();
				t.display = model.getName(e);
				t.value = e;
				TreeElement<T> oldTree = find(old, e);
				if(oldTree != null)t.showChildren = oldTree.showChildren;
				root.children.add(t);
				model.refresh(e);
				walkChildren(t, oldTree != null ? oldTree.children : null);
			});
		}

		private TreeElement<T> find(List<TreeElement<T>> list, T elem) {
			for (TreeElement<T> e : list) {
				if(e.value == elem)return e;
				else {
					TreeElement<T> d = find(e.children, elem);
					if(d != null)return d;
				}
			}
			return null;
		}

		private void walkChildren(TreeElement<T> p, List<TreeElement<T>> old) {
			model.getElements(p.value, e -> {
				TreeElement<T> t = new TreeElement<>();
				t.display = model.getName(e);
				t.value = e;
				p.children.add(t);
				TreeElement<T> oldTree = null;
				if(old != null)oldTree = find(old, e);
				if(oldTree != null)t.showChildren = oldTree.showChildren;
				walkChildren(t, oldTree != null ? oldTree.children : null);
			});
		}
	}
}
