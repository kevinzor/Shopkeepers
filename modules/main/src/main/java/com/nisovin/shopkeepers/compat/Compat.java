package com.nisovin.shopkeepers.compat;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Provides access to the {@link CompatProvider} implementation.
 */
public final class Compat {

	private static final Map<String, CompatVersion> COMPAT_VERSIONS = new LinkedHashMap<>();

	private static void register(CompatVersion version) {
		var compatVersion = version.getCompatVersion();
		if (COMPAT_VERSIONS.containsKey(compatVersion)) {
			throw new IllegalArgumentException("CompatVersion '" + compatVersion
					+ "' is already registered!");
		}

		COMPAT_VERSIONS.put(compatVersion, version);
	}

	// We have to update and rebuild our compat code whenever the mappings changed.
	// Changes to the mappings version do not necessarily align with a bump of the CraftBukkit
	// version. If the mappings changed without a bump of the CraftBukkit version, we only support
	// the latest mappings version: Our modules can only depend on specific CraftBukkit versions,
	// and building different build versions of CraftBukkit that share the same artifact version
	// overwrite each other inside the Maven repository. Also, we want to limit the number of
	// CraftBukkit versions we depend on and build.
	// Although they look similar, our compat versions do not necessarily match CraftBukkit's
	// 'Minecraft Version': Our revision number (behind the 'R') is incremented for every new compat
	// module for a specific major Minecraft version, which usually aligns with mappings updates for
	// new minor Minecraft updates, whereas CraftBukkit may increment its 'Minecraft Version' less
	// frequently. Also, our compat version may include additional tags, such as whether the module
	// is paper-specific.
	// Note: On Paper, since 1.21.6, the mappings version is no longer supported and we use the
	// Minecraft version instead.
	static {
		// Registered in the order from latest to oldest.
		// 1.21.8: Mappings version has not changed. We can reuse the 1.21.7 compat modules.
		register(new CompatVersion("1_21_R7_paper", Arrays.asList(
				new ServerVersion("1.21.7", "1.21.7"),
				new ServerVersion("1.21.8", "1.21.8")
				new ServerVersion("1.21.9", "1.21.9")
		)));
		register(new CompatVersion("1_21_R7", Arrays.asList(
				new ServerVersion("1.21.7", "98b42190c84edaa346fd96106ee35d6f"),
				new ServerVersion("1.21.8", "98b42190c84edaa346fd96106ee35d6f")
				new ServerVersion("1.21.9", "98b42190c84edaa346fd96106ee35d6f")
		)));
		register(new CompatVersion("1_21_R6_paper", "1.21.6", "1.21.6"));
		register(new CompatVersion("1_21_R6", "1.21.6", "164f8e872cb3dff744982fca079642b2"));
		register(new CompatVersion("1_21_R5_paper", "1.21.5", "7ecad754373a5fbc43d381d7450c53a5"));
		register(new CompatVersion("1_21_R5", "1.21.5", "7ecad754373a5fbc43d381d7450c53a5"));
		register(new CompatVersion("1_21_R4", "1.21.4", "60ac387ca8007aa018e6aeb394a6988c"));
		register(new CompatVersion("1_21_R3", "1.21.3", "61a218cda78417b6039da56e08194083"));
		// Note: 1.21.2 was immediately replaced by 1.21.3 and is not supported.
		register(new CompatVersion("1_21_R2", "1.21.1", "7092ff1ff9352ad7e2260dc150e6a3ec"));
		register(new CompatVersion("1_21_R1", "1.21", "229d7afc75b70a6c388337687ac4da1f"));
		// Note: MC 1.20.6 completely replaced 1.20.5. We only support 1.20.6.
		register(new CompatVersion("1_20_R5", "1.20.6", "ee13f98a43b9c5abffdcc0bb24154460"));
		register(new CompatVersion(
				FallbackCompatProvider.VERSION_ID,
				FallbackCompatProvider.VERSION_ID,
				FallbackCompatProvider.VERSION_ID
		));
	}

	public static @Nullable CompatVersion getCompatVersion(String compatVersion) {
		return COMPAT_VERSIONS.get(compatVersion); // Null if not found
	}

	/**
	 * Searches for a matching {@link CompatVersion}.
	 * 
	 * @param mappingsVersion
	 *            the mappings version
	 * @param variant
	 *            the variant, or an empty String
	 * @return the matched {@link CompatVersion}, or <code>null</code> if not suited
	 *         {@link CompatVersion} is found
	 */
	private static @Nullable CompatVersion findCompatVersion(String mappingsVersion, String variant) {
		var compatVersion = COMPAT_VERSIONS.values().stream()
				.filter(x -> x.getVariant().equals(variant)
						&& x.getSupportedServerVersions().stream()
								.anyMatch(v -> v.getMappingsVersion().equals(mappingsVersion)))
				.findFirst()
				.orElse(null);
		if (compatVersion == null && !variant.isEmpty()) {
			// Check again but also match compat versions without any variant:
			// This allows us to reuse the older compatible compat version implementations for Paper
			// servers without having to copy them.
			compatVersion = COMPAT_VERSIONS.values().stream()
					.filter(x -> !x.hasVariant()
							&& x.getSupportedServerVersions().stream()
									.anyMatch(v -> v.getMappingsVersion().equals(mappingsVersion)))
					.findFirst()
					.orElse(null);
		}
		return compatVersion;
	}

	// ----

	private static @Nullable CompatProvider provider;

	public static boolean hasProvider() {
		return (provider != null);
	}

	public static CompatProvider getProvider() {
		return Validate.State.notNull(provider, "Compat provider is not set up!");
	}

	// Returns true if the compat or fallback provider has been successfully set up.
	public static boolean load(Plugin plugin) {
		if (provider != null) {
			throw new IllegalStateException("Provider already loaded!");
		}

		if (isForceFallback(plugin)) {
			Log.warning("Force fallback: Shopkeepers is trying to run in 'fallback mode'.");
		} else {
			var mappingsVersion = ServerUtils.getMappingsVersion();
			var variant = ServerUtils.isPaper() ? CompatVersion.VARIANT_PAPER : "";

			var compatVersion = findCompatVersion(mappingsVersion, variant);
			if (compatVersion != null) {
				String compatVersionString = compatVersion.getCompatVersion();
				try {
					Class<?> clazz = Class.forName(
							"com.nisovin.shopkeepers.compat.v" + compatVersionString + ".CompatProviderImpl"
					);
					provider = (CompatProvider) clazz.getConstructor().newInstance();
					Log.info("Compatibility provider loaded: " + compatVersionString);
					return true; // Success
				} catch (Exception e) {
					Log.severe("Failed to load compatibility provider for version '"
							+ compatVersionString + "'!", e);
					// Continue with fallback.
				}
			}

			// Incompatible server version detected:
			Log.warning("Incompatible server version: " + Bukkit.getBukkitVersion() + " (mappings: "
					+ mappingsVersion + ", variant: " + (variant.isEmpty() ? "default" : variant)
					+ ")");
			Log.warning("Shopkeepers is trying to run in 'fallback mode'.");
			Log.info("Check for updates at: " + plugin.getDescription().getWebsite());
		}

		try {
			provider = new FallbackCompatProvider();
			return true; // Success
		} catch (Exception e) {
			Log.severe("Failed to enable 'fallback mode'!", e);
		}
		return false;
	}

	private static boolean isForceFallback(Plugin plugin) {
		var pluginDataFolder = plugin.getDataFolder().toPath();
		var forceFallbackFile = pluginDataFolder.resolve(".force-fallback");
		return Files.exists(forceFallbackFile);
	}
}
