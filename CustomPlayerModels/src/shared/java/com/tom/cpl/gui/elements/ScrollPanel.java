package com.tom.cpl.gui.elements;

import com.tom.cpl.gui.IGui;
import com.tom.cpl.gui.KeyboardEvent;
import com.tom.cpl.gui.MouseEvent;
import com.tom.cpl.math.Box;
import com.tom.cpl.math.MathHelper;

public class ScrollPanel extends Panel {
	private Panel display;
	private int xScroll, yScroll;
	private int enableDrag;
	private int dragX, dragY, xScOld, yScOld;
	private boolean scrollBarSide;

	public ScrollPanel(IGui gui) {
		super(gui);
	}

	@Override
	public void mouseWheel(MouseEvent event) {
		display.mouseWheel(event.offset(bounds).offset(-xScroll, -yScroll));
		if(!event.isConsumed() && event.isInBounds(bounds)) {
			float overflowX = bounds.w / (float) display.getBounds().w;
			float overflowY = bounds.h / (float) display.getBounds().h;
			int xe = -1;
			int ye = -1;
			if(overflowY < 1) {
				xe = 3;
			}
			if(overflowX < 1) {
				ye = 3;
			}
			if(gui.isShiftDown()) {
				int newScroll = xScroll - event.btn * 5;
				xScroll = MathHelper.clamp(newScroll, 0, Math.max(display.getBounds().w - bounds.w + xe, 0));
			} else {
				int newScroll = yScroll - event.btn * 5;
				yScroll = MathHelper.clamp(newScroll, 0, Math.max(display.getBounds().h - bounds.h + ye, 0));
			}
			event.consume();
		}
	}

	public void setDisplay(Panel display) {
		this.display = display;
	}

	@Override
	public void draw(MouseEvent evt, float partialTicks) {
		gui.pushMatrix();
		Box bounds = getBounds();
		gui.setPosOffset(bounds);
		if(display.backgroundColor != 0)
			gui.drawBox(0, 0, bounds.w, bounds.h, display.backgroundColor);
		gui.setupCut();
		Box b = display.getBounds();
		gui.setPosOffset(new Box(-xScroll, -yScroll, b.w, b.h));
		display.draw(evt.offset(bounds.x - xScroll, bounds.y - yScroll), partialTicks);
		gui.popMatrix();
		gui.pushMatrix();
		gui.setPosOffset(bounds);
		gui.setupCut();
		float overflowX = bounds.w / (float) display.getBounds().w;
		float overflowY = bounds.h / (float) display.getBounds().h;
		int scx = scrollBarSide ? 0 : bounds.w - 3;
		int scy = bounds.h - 4;
		if(overflowY < 1) {
			float h = Math.max(overflowY * bounds.h, 8);
			float scroll = yScroll / (float) (display.getBounds().h - bounds.h);
			int y = (int) (scroll * (bounds.h - h));
			Box bar = new Box(bounds.x + scx, bounds.y + y, 3, (int) h);
			gui.drawBox(scx, 0, 3, bounds.h, gui.getColors().panel_background);
			gui.drawBox(scx, y, 3, h, evt.isHovered(bar) || enableDrag == 1 ? gui.getColors().button_hover : gui.getColors().button_disabled);
		}
		if(overflowX < 1) {
			float w = Math.max(overflowX * bounds.w, 8);
			float scroll = xScroll / (float) (display.getBounds().w - bounds.w);
			int x = (int) (scroll * (bounds.w - w));
			Box bar = new Box(bounds.x + x, bounds.y + scy, (int) w, 3);
			gui.drawBox(x, scy, w, 4, gui.getColors().panel_background);
			gui.drawBox(x, scy, w, 4, evt.isHovered(bar) || enableDrag == 2 ? gui.getColors().button_hover : gui.getColors().button_disabled);
		}
		gui.popMatrix();
		gui.setupCut();
	}

	@Override
	public void mouseClick(MouseEvent event) {
		if(event.offset(bounds).isHovered(new Box(scrollBarSide ? 0 : bounds.w - 3, 0, 3, bounds.h))) {
			dragX = event.x;
			dragY = event.y;
			xScOld = xScroll;
			yScOld = yScroll;
			enableDrag = 1;
			event.consume();
		}
		if(event.offset(bounds).isHovered(new Box(0, bounds.h - 4, bounds.w, 3))) {
			dragX = event.x;
			dragY = event.y;
			xScOld = xScroll;
			yScOld = yScroll;
			enableDrag = 2;
			event.consume();
		}
		MouseEvent e = event.offset(bounds).offset(-xScroll, -yScroll);
		if(!event.isInBounds(bounds)) {
			e = e.cancelled();
		}
		display.mouseClick(e);
	}

	@Override
	public void keyPressed(KeyboardEvent event) {
		display.keyPressed(event);
	}

	@Override
	public void mouseDrag(MouseEvent event) {
		if(enableDrag != 0) {
			float overflowX = bounds.w / (float) display.getBounds().w;
			float overflowY = bounds.h / (float) display.getBounds().h;
			int xe = -1;
			int ye = -1;
			if(overflowY < 1) {
				xe = 3;
			}
			if(overflowX < 1) {
				ye = 3;
			}
			switch (enableDrag) {
			case 1:
			{
				int drag = (int) ((event.y - dragY) / (float) bounds.h * display.getBounds().h);
				int newScroll = yScOld + drag;
				yScroll = MathHelper.clamp(newScroll, 0, Math.max(display.getBounds().h - bounds.h + ye, 0));
			}
			break;

			case 2:
			{
				int drag = (int) ((event.x - dragX) / (float) bounds.w * display.getBounds().w);
				int newScroll = xScOld + drag;
				xScroll = MathHelper.clamp(newScroll, 0, Math.max(display.getBounds().w - bounds.w + xe, 0));
			}
			break;

			default:
				break;
			}
			event.consume();
		} else
			display.mouseDrag(event.offset(bounds).offset(-xScroll, -yScroll));
	}

	@Override
	public void mouseRelease(MouseEvent event) {
		if(enableDrag != 0) {
			enableDrag = 0;
			event.consume();
		} else
			display.mouseRelease(event.offset(bounds).offset(-xScroll, -yScroll));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(display != null)display.setVisible(visible);
	}

	public void setScrollBarSide(boolean scrollBarSide) {
		this.scrollBarSide = scrollBarSide;
	}

	public void setScrollX(int xScroll) {
		this.xScroll = xScroll;
	}

	public void setScrollY(int yScroll) {
		this.yScroll = yScroll;
	}

	public int getScrollX() {
		return xScroll;
	}

	public int getScrollY() {
		return yScroll;
	}

	public Panel getDisplay() {
		return display;
	}
}
