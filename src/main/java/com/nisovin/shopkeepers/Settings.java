package com.nisovin.shopkeepers;

import static com.nisovin.shopkeepers.config.ConfigHelper.loadConfigValue;
import static com.nisovin.shopkeepers.config.ConfigHelper.setConfigValue;
import static com.nisovin.shopkeepers.config.ConfigHelper.toConfigKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.config.ConfigLoadException;
import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class Settings {

	public static final class DebugOptions {
		private DebugOptions() {
		}

		// Logs details of the server version dependent capabilities.
		public static final String capabilities = "capabilities";
		// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get combined into a
		// single logging entry to slightly reduce spam.
		public static final String logAllEvents = "log-all-events";
		// Prints the registered listeners for the first call of each event.
		public static final String printListeners = "print-listeners";
		// Enables debugging output related to shopkeeper activation.
		public static final String shopkeeperActivation = "shopkeeper-activation";
		// Enables additional commands related debugging output.
		public static final String commands = "commands";
		// Logs information when updating stored shop owner names.
		public static final String ownerNameUpdates = "owner-name-updates";
		// Logs whenever a shopkeeper performs item migrations (eg. for trading offers).
		public static final String itemMigrations = "item-migrations";
		// Logs whenever we explicitly convert items to Spigot's data format. Note that this does not log when items get
		// implicitly converted, which may happen under various circumstances.
		public static final String itemConversions = "item-conversions";
	}

	public static boolean isDebugging() {
		return isDebugging(null);
	}

	public static boolean isDebugging(String option) {
		if (Bukkit.isPrimaryThread()) {
			return Settings.debug && (option == null || Settings.debugOptions.contains(option));
		} else {
			AsyncSettings async = Settings.async();
			return async.debug && (option == null || async.debugOptions.contains(option));
		}
	}

	// Cached values for settings which are used asynchronously.
	public static final class AsyncSettings {

		private static volatile AsyncSettings INSTANCE = new AsyncSettings();

		private static void refresh() {
			INSTANCE = new AsyncSettings();
		}

		public final boolean debug;
		public final List<String> debugOptions;
		public final String fileEncoding;

		private AsyncSettings() {
			this.debug = Settings.debug;
			this.debugOptions = new ArrayList<>(Settings.debugOptions);
			this.fileEncoding = Settings.fileEncoding;
		}
	}

	public static AsyncSettings async() {
		return AsyncSettings.INSTANCE;
	}

	/*
	 * General Settings
	 */
	public static int configVersion = 4;
	public static boolean debug = false;
	// See DebugOptions for all available options.
	public static List<String> debugOptions = new ArrayList<>(0);
	public static boolean enableMetrics = true;

	/*
	 * Messages
	 */
	public static String language = "en-default";

	/*
	 * Shopkeeper Data
	 */
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	public static boolean convertPlayerItems = false;
	public static boolean convertAllPlayerItems = true;
	public static List<ItemData> convertPlayerItemsExceptions = new ArrayList<>();

	/*
	 * Plugin Compatibility
	 */
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(Material.VILLAGER_SPAWN_EGG, "&aShopkeeper", null);
	public static boolean preventShopCreationItemRegularUsage = true;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireContainerRecentlyPlaced = true;
	public static int maxContainerDistance = 15;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";

	public static boolean protectContainers = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakContainer = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	public static List<String> enabledLivingShops = Arrays.asList(
			EntityType.VILLAGER.name(),
			EntityType.COW.name(),
			EntityType.MUSHROOM_COW.name(),
			EntityType.SHEEP.name(),
			EntityType.PIG.name(),
			EntityType.CHICKEN.name(),
			EntityType.OCELOT.name(),
			EntityType.RABBIT.name(),
			EntityType.WOLF.name(),
			EntityType.SNOWMAN.name(),
			EntityType.IRON_GOLEM.name(),
			EntityType.BLAZE.name(),
			EntityType.SILVERFISH.name(),
			EntityType.POLAR_BEAR.name(), // MC 1.10
			EntityType.SKELETON.name(),
			EntityType.STRAY.name(), // MC 1.11
			EntityType.WITHER_SKELETON.name(), // MC 1.11
			EntityType.SPIDER.name(),
			EntityType.CAVE_SPIDER.name(),
			EntityType.CREEPER.name(),
			EntityType.WITCH.name(),
			EntityType.ENDERMAN.name(),
			EntityType.ZOMBIE.name(),
			EntityType.ZOMBIE_VILLAGER.name(), // MC 1.11
			EntityType.HUSK.name(), // MC 1.11
			EntityType.GIANT.name(),
			EntityType.GHAST.name(),
			EntityType.SLIME.name(),
			EntityType.MAGMA_CUBE.name(),
			EntityType.SQUID.name(),
			EntityType.HORSE.name(),
			EntityType.MULE.name(),
			EntityType.DONKEY.name(),
			EntityType.SKELETON_HORSE.name(),
			EntityType.ZOMBIE_HORSE.name(),
			EntityType.EVOKER.name(), // MC 1.11
			EntityType.VEX.name(), // MC 1.11
			EntityType.VINDICATOR.name(), // MC 1.11
			EntityType.ILLUSIONER.name(), // MC 1.12
			EntityType.PARROT.name(), // MC 1.12
			EntityType.TURTLE.name(), // MC 1.13
			EntityType.PHANTOM.name(), // MC 1.13
			EntityType.COD.name(), // MC 1.13
			EntityType.SALMON.name(), // MC 1.13
			EntityType.PUFFERFISH.name(), // MC 1.13
			EntityType.TROPICAL_FISH.name(), // MC 1.13
			EntityType.DROWNED.name(), // MC 1.13
			EntityType.DOLPHIN.name(), // MC 1.13
			EntityType.CAT.name(), // MC 1.14
			EntityType.PANDA.name(), // MC 1.14
			EntityType.PILLAGER.name(), // MC 1.14
			EntityType.RAVAGER.name(), // MC 1.14
			EntityType.LLAMA.name(), // MC 1.11
			EntityType.TRADER_LLAMA.name(), // MC 1.14
			EntityType.WANDERING_TRADER.name(), // MC 1.14
			EntityType.FOX.name(), // MC 1.14
			"BEE", // MC 1.15
			"ZOMBIFIED_PIGLIN", // MC 1.16, replaced PIG_ZOMBIE
			"PIGLIN", // MC 1.16
			"HOGLIN", // MC 1.16
			"ZOGLIN", // MC 1.16
			"STRIDER", // MC 1.16
			"PIGLIN_BRUTE" // MC 1.16.2
	);

	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;
	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";

	public static boolean enableCitizenShops = true;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;
	public static String signShopFirstLine = "[SHOP]";

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static String editorTitle = "Shopkeeper Editor";

	public static int maxTradesPages = 5;

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);

	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableContainerOptionOnPlayerShop = true;
	public static ItemData containerItem = new ItemData(Material.CHEST);

	public static ItemData deleteItem = new ItemData(Material.BONE);

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean disableZombieVillagerCuring = false;
	public static boolean hireOtherVillagers = false;

	public static boolean disableWanderingTraders = false;
	public static boolean blockWanderingTraderSpawns = false;
	public static boolean hireWanderingTraders = false;

	public static boolean editRegularVillagers = true;
	public static boolean editRegularWanderingTraders = true;

	/*
	 * Hiring
	 */
	public static ItemData hireItem = new ItemData(Material.EMERALD);
	public static int hireOtherVillagersCosts = 1;
	public static String forHireTitle = "For Hire";
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean incrementVillagerStatistics = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData zeroCurrencyItem = new ItemData(Material.BARRIER);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);
	public static ItemData zeroHighCurrencyItem = new ItemData(Material.BARRIER);

	// Note: This can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	// /////

	// These String / String list settings are exempt from color conversion:
	private static final Set<String> noColorConversionKeys = new HashSet<>(Arrays.asList(
			toConfigKey("debugOptions"), toConfigKey("fileEncoding"), toConfigKey("shopCreationItemSpawnEggEntityType"),
			toConfigKey("maxShopsPermOptions"), toConfigKey("enabledLivingShops"), toConfigKey("nameRegex"),
			toConfigKey("language")));

	// Returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) throws ConfigLoadException {
		boolean configChanged = false;

		// Perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(config);
		if (migrated) {
			configChanged = true;
		}

		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.isSynthetic()) continue;
				if (!Modifier.isPublic(field.getModifiers())) {
					continue;
				}
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());

				// Initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					Log.warning("Config: Inserting default value for missing config entry: " + configKey);

					// Determine default value:
					Configuration defaults = config.getDefaults();
					Object defaultValue = loadConfigValue(defaults, configKey, noColorConversionKeys, typeClass, genericType);

					// Validate default value:
					if (defaultValue == null) {
						Log.warning("Config: Missing default value for missing config entry: " + configKey);
						continue;
					} else if (!Utils.isAssignableFrom(typeClass, defaultValue.getClass())) {
						Log.warning("Config: Default value for missing config entry '" + configKey + "' is of wrong type: "
								+ "Got " + defaultValue.getClass().getName() + ", expecting " + typeClass.getName());
						continue;
					}

					// Set default value:
					setConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType, defaultValue);
					configChanged = true;
				}

				// Load value:
				Object value = loadConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType);
				field.set(null, value);
			}
		} catch (Exception e) {
			throw new ConfigLoadException("Error while loading config values!", e);
		}

		// Validation:

		boolean foundInvalidEntityType = false;
		boolean removePigZombie = false;
		for (String entityTypeId : enabledLivingShops) {
			EntityType entityType = matchEntityType(entityTypeId);
			if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
				foundInvalidEntityType = true;
				if ("PIG_ZOMBIE".equals(entityTypeId)) {
					removePigZombie = true;
				} else {
					Log.warning("Config: Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
				}
			}
		}
		// Migration for MC 1.16 TODO remove this again at some point
		if (removePigZombie) {
			Log.warning("Config: The mob type 'PIG_ZOMBIE' no longer exist since MC 1.16 and has therefore been removed from the 'enabled-living-shops'. Consider replacing it with 'ZOMBIFIED_PIGLIN'.");
			enabledLivingShops.removeIf(e -> Objects.equals(e, "PIG_ZOMBIE"));
			config.set(toConfigKey("enabledLivingShops"), enabledLivingShops);
			configChanged = true;
		}
		if (foundInvalidEntityType) {
			Log.warning("Config: All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
		}

		if (maxContainerDistance > 50) {
			Log.warning("Config: 'max-container-distance' can be at most 50.");
			maxContainerDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning("Config: 'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		// Certain items cannot be of type AIR:
		if (shopCreationItem.getType() == Material.AIR) {
			Log.warning("Config: 'shop-creation-item' can not be AIR.");
			shopCreationItem = shopCreationItem.withType(Material.VILLAGER_SPAWN_EGG);
		}
		if (hireItem.getType() == Material.AIR) {
			Log.warning("Config: 'hire-item' can not be AIR.");
			hireItem = hireItem.withType(Material.EMERALD);
		}
		if (currencyItem.getType() == Material.AIR) {
			Log.warning("Config: 'currency-item' can not be AIR.");
			currencyItem = currencyItem.withType(Material.EMERALD);
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem.getType() == Material.AIR) {
				Log.warning("Config: 'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
		}
		if (maxTradesPages < 1) {
			Log.warning("Config: 'max-trades-pages' can not be less than 1!");
			maxTradesPages = 1;
		} else if (maxTradesPages > 10) {
			Log.warning("Config: 'max-trades-pages' can not be greater than 10!");
			maxTradesPages = 10;
		}
		if (taxRate < 0) {
			Log.warning("Config: 'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning("Config: 'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}

		onSettingsChanged();
		return configChanged;
	}

	public static void onSettingsChanged() {
		// Prepare derived settings:
		DerivedSettings.setup();

		// Refresh async settings cache:
		AsyncSettings.refresh();
	}

	// Item utilities:

	// Stores derived settings which get setup after loading the config.
	public static class DerivedSettings {

		public static ItemData namingItemData = new ItemData(Material.AIR);

		// Button items:
		public static ItemData nameButtonItem = new ItemData(Material.AIR);
		public static ItemData containerButtonItem = new ItemData(Material.AIR);
		public static ItemData deleteButtonItem = new ItemData(Material.AIR);
		public static ItemData hireButtonItem = new ItemData(Material.AIR);

		public static ItemData deleteVillagerButtonItem = new ItemData(Material.AIR);
		public static ItemData villagerInventoryButtonItem = new ItemData(Material.AIR);

		public static Pattern shopNamePattern = Pattern.compile("^[A-Za-z0-9 ]{3,32}$");

		// Gets called after the config has been loaded:
		private static void setup() {
			// Ignore display name (which is used for specifying the new shopkeeper name):
			namingItemData = new ItemData(ItemUtils.setItemStackName(nameItem.createItemStack(), null));

			// Button items:
			nameButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(nameItem.createItemStack(), Messages.buttonName, Messages.buttonNameLore));
			containerButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(containerItem.createItemStack(), Messages.buttonContainer, Messages.buttonContainerLore));
			deleteButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), Messages.buttonDelete, Messages.buttonDeleteLore));
			hireButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(hireItem.createItemStack(), Messages.buttonHire, Messages.buttonHireLore));

			// Note: These use the same item types as the corresponding shopkeeper buttons.
			deleteVillagerButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), Messages.buttonDeleteVillager, Messages.buttonDeleteVillagerLore));
			villagerInventoryButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(containerItem.createItemStack(), Messages.buttonVillagerInventory, Messages.buttonVillagerInventoryLore));

			try {
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			} catch (PatternSyntaxException e) {
				Log.warning("Config: 'name-regex' is not a valid regular expression ('" + Settings.nameRegex + "'). Reverting to default.");
				Settings.nameRegex = "[A-Za-z0-9 ]{3,32}";
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			}
		}
	}

	// Shop creation item:
	public static ItemStack createShopCreationItem() {
		return shopCreationItem.createItemStack();
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return shopCreationItem.matches(item);
	}

	// Naming item:
	public static boolean isNamingItem(ItemStack item) {
		return DerivedSettings.namingItemData.matches(item);
	}

	public static ItemStack createNameButtonItem() {
		return DerivedSettings.nameButtonItem.createItemStack();
	}

	// Container button:
	public static ItemStack createContainerButtonItem() {
		return DerivedSettings.containerButtonItem.createItemStack();
	}

	// Delete button:
	public static ItemStack createDeleteButtonItem() {
		return DerivedSettings.deleteButtonItem.createItemStack();
	}

	// Hire item:
	public static ItemStack createHireButtonItem() {
		return DerivedSettings.hireButtonItem.createItemStack();
	}

	public static boolean isHireItem(ItemStack item) {
		return hireItem.matches(item);
	}

	// CURRENCY

	// Currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return currencyItem.createItemStack(amount);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return currencyItem.matches(item);
	}

	// High currency item:
	public static boolean isHighCurrencyEnabled() {
		return (highCurrencyValue > 0 && highCurrencyItem.getType() != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return highCurrencyItem.createItemStack(amount);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return highCurrencyItem.matches(item);
	}

	// Zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (zeroCurrencyItem.getType() == Material.AIR) return null;
		return zeroCurrencyItem.createItemStack();
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (zeroCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroCurrencyItem.matches(item);
	}

	// Zero high currency item:
	public static ItemStack createZeroHighCurrencyItem() {
		if (zeroHighCurrencyItem.getType() == Material.AIR) return null;
		return zeroHighCurrencyItem.createItemStack();
	}

	public static boolean isZeroHighCurrencyItem(ItemStack item) {
		if (zeroHighCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroHighCurrencyItem.matches(item);
	}

	//

	public static int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (PermissionUtils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	public static EntityType matchEntityType(String entityTypeId) {
		if (StringUtils.isEmpty(entityTypeId)) return null;
		// Get by Bukkit id:
		String normalizedEntityTypeId = entityTypeId.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		try {
			return EntityType.valueOf(normalizedEntityTypeId);
		} catch (IllegalArgumentException e) {
			// Unknown entity type:
			return null;
		}
	}

	private Settings() {
	}
}
