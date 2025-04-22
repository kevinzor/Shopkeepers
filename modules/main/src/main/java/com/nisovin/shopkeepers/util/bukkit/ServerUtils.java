package com.nisovin.shopkeepers.util.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class ServerUtils {

	private static final boolean IS_PAPER;
	private static final String MAPPINGS_VERSION;

	static {
		boolean isPaper;
		try {
			Class.forName("io.papermc.paper.registry.RegistryAccess");
			isPaper = true;
		} catch (ClassNotFoundException e) {
			isPaper = false;
		}
		IS_PAPER = isPaper;

		UnsafeValues unsafeValues = Bukkit.getUnsafe();
		Method getMappingsVersionMethod;
		try {
			getMappingsVersionMethod = unsafeValues.getClass().getDeclaredMethod("getMappingsVersion");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(
					"Could not find method 'getMappingsVersion' in the UnsafeValues implementation!",
					e
			);
		}
		try {
			MAPPINGS_VERSION = Unsafe.cast(getMappingsVersionMethod.invoke(unsafeValues));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not retrieve the server's mappings version!", e);
		}

	}

	/**
	 * Checks if the server has access to Paper-specific API.
	 * 
	 * @return <code>true</code> if the server provides the Paper API
	 */
	public static boolean isPaper() {
		return IS_PAPER;
	}

	public static String getMappingsVersion() {
		return MAPPINGS_VERSION;
	}

	public static String getCraftBukkitPackage() {
		Package pkg = Unsafe.assertNonNull(Bukkit.getServer().getClass().getPackage());
		return pkg.getName();
	}

	private ServerUtils() {
	}
}
