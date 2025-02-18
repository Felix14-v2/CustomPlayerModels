package com.tom.cpm.shared.animation.interpolator;

import java.util.function.DoubleUnaryOperator;

import com.tom.cpl.math.MathHelper;
import com.tom.cpm.shared.animation.InterpolatorChannel;

public class LinearInterpolator implements Interpolator {
	private float[] values;

	@Override
	public double applyAsDouble(double operand) {
		int frm = Math.abs(MathHelper.floor(operand));
		return MathHelper.lerp((float) (operand - Math.floor(operand)), values[Math.min(frm, values.length - 1)], values[Math.min(frm + 1, values.length - 1)]);
	}

	@Override
	public void init(float[] values, InterpolatorChannel channel) {
		DoubleUnaryOperator setup = channel.createInterpolatorSetup();
		this.values = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			this.values[i] = (float) setup.applyAsDouble(values[i]);
		}
	}
}
