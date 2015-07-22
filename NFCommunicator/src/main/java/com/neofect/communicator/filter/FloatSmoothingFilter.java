package com.neofect.communicator.filter;

import java.util.ArrayList;
import java.util.List;

public class FloatSmoothingFilter implements SmoothingFilter {
	
	private List<Float>	sampleValues = new ArrayList<Float>();
	private double		sumOfSampleValues = 0;
	
	@Override
	public boolean add(float value, int samplingSize) {
		sampleValues.add(value);
		sumOfSampleValues += value;
		if(sampleValues.size() < samplingSize) {
			return false;
		}

		// Remove the oldest value
		while(sampleValues.size() > samplingSize) {
			sumOfSampleValues -= sampleValues.get(0);
			sampleValues.remove(0);
		}
		return true;
	}
	
	@Override
	public float getAverage() {
		if(sampleValues.size() == 0) {
			throw new IllegalStateException("There is no sample gathered!");
		}
		return (float) (sumOfSampleValues / sampleValues.size());
	}
	
	@Override
	public int getNumberOfSamples() {
		return sampleValues.size();
	}

	@Override
	public void clearSamples() {
		sampleValues.clear();
	}

}
