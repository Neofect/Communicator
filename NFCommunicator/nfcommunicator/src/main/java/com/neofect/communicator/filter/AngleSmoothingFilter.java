package com.neofect.communicator.filter;

public class AngleSmoothingFilter implements SmoothingFilter {
	
	private FloatSmoothingFilter sinFilter = new FloatSmoothingFilter();
	private FloatSmoothingFilter cosFilter = new FloatSmoothingFilter();

	/**
	 * The angle range is -180 ~ 180 as default. The range can be changed by this min value.
	 * If min value is -90, then the range is -90 ~ 270.
	 */
	private float minValue = -180f;
	
	@Override
	public boolean add(float angleInDegree, int samplingSize) {
		float radian = (float) Math.toRadians(angleInDegree);
		float sinValue = (float) Math.sin(radian);
		float cosValue = (float) Math.cos(radian);
		boolean enoughSamples = sinFilter.add(sinValue, samplingSize);
		cosFilter.add(cosValue, samplingSize);
		return enoughSamples;
	}
	
	@Override
	public float getAverage() {
		float sinAverage = sinFilter.getAverage();
		float cosAverage = cosFilter.getAverage();
		
		float radian = (float) Math.atan2(sinAverage, cosAverage);
		float degree = (float) Math.toDegrees(radian);
		if (degree < minValue) {
			degree += 360;
		} else if (degree > minValue + 360) {
			degree -= 360;
		}
		return degree;
	}
	
	@Override
	public int getNumberOfSamples() {
		return sinFilter.getNumberOfSamples();
	}

	@Override
	public void clearSamples() {
		sinFilter.clearSamples();
		cosFilter.clearSamples();
	}

	public void setMinValue(float minValue) {
		if (minValue < -360) {
			throw new IllegalArgumentException("Min value must not be smaller than -360.");
		} else if (minValue > 0) {
			throw new IllegalArgumentException("Min value must not be larger than 0.");
		}
		this.minValue = minValue;
	}
}
