package com.neofect.communicator;

import com.neofect.communicator.message.CommunicationMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/**
 * Created by Neo on 2017. 10. 22..
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class CommunicatorTest {

	private static class DummyDevice extends Device {
		DummyDevice(Connection connection) { super(connection); }
		@Override protected boolean processMessage(CommunicationMessage message) { return false; }
	}

	private static class DummySubclassDevice extends DummyDevice {
		DummySubclassDevice(Connection connection) { super(connection); }
		@Override protected boolean processMessage(CommunicationMessage message) { return false; }
	}

	@Test
	public void testDeviceClass() {
		Class<? extends Device> dummyDeviceClass = DummyDevice.class;
		Class<? extends Device> dummySubclassDeviceClass = DummySubclassDevice.class;

		Class<?> clazz = dummySubclassDeviceClass;
		while (clazz != Device.class) {
			if (clazz == dummyDeviceClass) {
				assertTrue(true);
				System.out.println("Class=" + clazz + ", true");
				return;
			}
			System.out.println("Class=" + clazz + ", false");
			clazz = clazz.getSuperclass();
		}
		assertTrue(false);
	}

}
