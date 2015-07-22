package com.neofect.communicator.filter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SmoothingFilterGroup<T extends SmoothingFilter> {

	private List<T> filters;
	
	public SmoothingFilterGroup(int numberOfFilters) {
		if(numberOfFilters <= 0) {
			throw new IllegalArgumentException("The number of filters must be positive!");
		}
		filters = new ArrayList<T>();
		Class<? extends SmoothingFilter> filterClass = getClassFromGeneric(this);
		for (int i = 0; i < numberOfFilters; ++i) {
			filters.add(createFilterInstance(filterClass));
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends SmoothingFilter> Class<T> getClassFromGeneric(SmoothingFilterGroup<T> filterGroup) {
		try {
			Type superClass = filterGroup.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to get parameterized class type from the given generic!", e);
		}
	}

	@SuppressWarnings("unchecked")
	private T createFilterInstance(Class<? extends SmoothingFilter> filterClass) {
		try {
			return (T) filterClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create a filter instance in group!", e);
		}
	}
	
	public boolean add(float[] values, int samplingSize) {
		if(values.length != filters.size()) {
			throw new IllegalArgumentException("The size of input values are not matching the filter's! filter=" + filters.size() + ", input=" + values.length);
		}
		
		boolean enoughSamples = false;
		for(int i = 0; i < filters.size(); ++i) {
			enoughSamples = filters.get(i).add(values[i], samplingSize);
		}
		return enoughSamples;
	}
	
	public float[] getAverageValues() {
		float[] filteredResult = new float[filters.size()];
		for(int i = 0; i < filters.size(); ++i) {
			float filteredValue = filters.get(i).getAverage();
			filteredResult[i] = filteredValue;
		}
		return filteredResult;
	}
	
	public void clearAccumulatedValues() {
		for(SmoothingFilter filter : filters) {
			filter.clearSamples();
		}
	}
	
	public int getNumberOfSamples() {
		return filters.get(0).getNumberOfSamples();
	}

}
