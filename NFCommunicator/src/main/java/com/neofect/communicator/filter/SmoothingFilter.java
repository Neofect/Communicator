package com.neofect.communicator.filter;


public interface SmoothingFilter {
	
	/**
	 * Returns true if the number of accumulated samples reaches the sampling size.
	 * @param value
	 * @param samplingSize
	 * @return
	 */
	boolean	add(float value, int samplingSize);
	
	float	getAverage();
	
	int		getNumberOfSamples();
	
	void	clearSamples();

}
