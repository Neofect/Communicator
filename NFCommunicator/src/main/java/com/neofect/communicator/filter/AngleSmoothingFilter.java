package com.neofect.communicator.filter;

public class AngleSmoothingFilter implements SmoothingFilter {
	
	private FloatSmoothingFilter sinFilter = new FloatSmoothingFilter();
	private FloatSmoothingFilter cosFilter = new FloatSmoothingFilter();
	
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
		return (float) Math.toDegrees(radian);
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

}
