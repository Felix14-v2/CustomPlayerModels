package com.tom.cpm.shared.model.render;

public enum RenderMode {
	NORMAL,
	GLOW,
	OUTLINE,
	COLOR,
	COLOR_GLOW,
	DEFAULT,
	PAINT,
	;

	public RenderMode glow() {
		if(this == COLOR)return COLOR_GLOW;
		return GLOW;
	}
}
