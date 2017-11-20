package com.neofect.communicator;

import com.neofect.communicator.message.Message;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Neo on 2017. 10. 22..
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class CommunicatorTest {

	private static class DummyDevice extends Device {
		DummyDevice(Connection connection) { super(connection); }
		@Override protected boolean processMessage(Message message) { return false; }
	}

	private static class DummySubclassDevice extends DummyDevice {
		DummySubclassDevice(Connection connection) { super(connection); }
		@Override protected boolean processMessage(Message message) { return false; }
	}

	@Test
	public void testDeviceClassSubclassing() {
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

	@Test
	public void testDeviceClassSubclassing2() {
		boolean result = Communicator.isSameOrSuperClassDevice(DummyDevice.class, DummyDevice.class);
		assertTrue(result);

		result = Communicator.isSameOrSuperClassDevice(Device.class, Device.class);
		assertTrue(result);

		result = Communicator.isSameOrSuperClassDevice(DummySubclassDevice.class, DummyDevice.class);
		assertTrue(result);

		result = Communicator.isSameOrSuperClassDevice(DummyDevice.class, DummySubclassDevice.class);
		assertFalse(result);

		result = Communicator.isSameOrSuperClassDevice(DummySubclassDevice.class, Device.class);
		assertTrue(result);

		result = Communicator.isSameOrSuperClassDevice(Device.class, DummySubclassDevice.class);
		assertFalse(result);
	}

}
