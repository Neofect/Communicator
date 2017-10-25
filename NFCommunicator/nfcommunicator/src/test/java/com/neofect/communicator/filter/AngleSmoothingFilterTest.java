package com.neofect.communicator.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * @author neo.kim@neofect.com
 * @date Oct 25, 2017
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class AngleSmoothingFilterTest {

	@Test
	public void testMinValue() {
		AngleSmoothingFilter filter = new AngleSmoothingFilter();

		for (int i = 0; i < 10; ++i) {
			filter.add(200, 3);
		}
		filter.setMinValue(-180);
		assertEquals(-160, filter.getAverage(), 0);

		filter.setMinValue(-270);
		assertEquals(-160, filter.getAverage(), 0);

		filter.setMinValue(-90);
		assertEquals(200, filter.getAverage(), 0);

		filter.setMinValue(-180);
		for (int i = 0; i < 10; ++i) {
			filter.add(-200, 3);
		}
		filter.setMinValue(-180);
		assertEquals(160, filter.getAverage(), 0);

		filter.setMinValue(-270);
		assertEquals(-200, filter.getAverage(), 0);

		filter.setMinValue(-90);
		assertEquals(160, filter.getAverage(), 0);
	}

}
