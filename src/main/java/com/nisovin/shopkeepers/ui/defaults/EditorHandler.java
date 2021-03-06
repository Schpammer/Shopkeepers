package com.nisovin.shopkeepers.ui.defaults;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.ShopkeeperUIHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class EditorHandler extends AbstractEditorHandler implements ShopkeeperUIHandler {

	private final AbstractShopkeeper shopkeeper;

	protected EditorHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	protected String getEditorTitle() {
		return Settings.editorTitle;
	}

	// EDITOR BUTTONS

	// A button which may reference the shopkeeper.
	public abstract static class ShopkeeperButton extends Button {

		public ShopkeeperButton() {
			this(false);
		}

		public ShopkeeperButton(boolean placeAtEnd) {
			super(placeAtEnd);
		}

		protected Shopkeeper getShopkeeper() {
			assert this.getEditorHandler() instanceof EditorHandler;
			return ((EditorHandler) this.getEditorHandler()).getShopkeeper();
		}
	}

	// For simple one-click actions which may reference the shopkeeper.
	public static abstract class ShopkeeperActionButton extends ActionButton {

		public ShopkeeperActionButton() {
			this(false);
		}

		public ShopkeeperActionButton(boolean placeAtEnd) {
			super(placeAtEnd);
		}

		@Override
		protected boolean isApplicable(AbstractEditorHandler editorHandler) {
			return super.isApplicable(editorHandler) && (editorHandler instanceof EditorHandler);
		}

		protected Shopkeeper getShopkeeper() {
			assert this.getEditorHandler() instanceof EditorHandler;
			return ((EditorHandler) this.getEditorHandler()).getShopkeeper();
		}

		@Override
		protected void onActionSuccess(InventoryClickEvent clickEvent, Player player) {
			Shopkeeper shopkeeper = this.getShopkeeper();

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// Save:
			shopkeeper.save();
		}
	}

	@Override
	protected ItemStack createTradeSetupIcon() {
		ShopType<?> shopType = this.getShopkeeper().getType();
		String itemName = TextUtils.replaceArguments(Messages.tradeSetupDescHeader, "shopType", shopType.getDisplayName());
		List<String> itemLore = shopType.getTradeSetupDescription();
		return ItemUtils.setItemStackNameAndLore(Settings.tradeSetupItem.createItemStack(), itemName, itemLore);
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		this.addButtonOrIgnore(this.createDeleteButton());
		this.addButtonOrIgnore(this.createNamingButton());
		this.addButtonOrIgnore(this.createContainerButton());
		this.addButtonsOrIgnore(shopkeeper.getShopObject().getEditorButtons());
	}

	protected Button createDeleteButton() {
		return new ShopkeeperButton(true) {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createDeleteButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Call event:
				PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(shopkeeper, player);
				Bukkit.getPluginManager().callEvent(deleteEvent);
				if (!deleteEvent.isCancelled()) {
					// Delete shopkeeper:
					shopkeeper.delete(player);

					// Save:
					shopkeeper.save();
				}
			}
		};
	}

	protected Button createNamingButton() {
		boolean useNamingButton = true;
		if (shopkeeper.getType() instanceof PlayerShopType) {
			// Naming via button enabled?
			if (Settings.namingOfPlayerShopsViaItem) {
				useNamingButton = false;
			} else {
				// No naming button for Citizens player shops if renaming is disabled for those.
				// TODO Restructure this to allow for dynamic editor buttons depending on shop (object) types and
				// settings.
				if (!Settings.allowRenamingOfPlayerNpcShops && shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
					useNamingButton = false;
				}
			}
		}
		if (!useNamingButton) return null;

		return new ShopkeeperButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createNameButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Also triggers save:
				getUISession(player).closeDelayed();

				// Start naming:
				SKShopkeepersPlugin.getInstance().getShopkeeperNaming().startNaming(player, shopkeeper);
				TextUtils.sendMessage(player, Messages.typeNewName);
			}
		};
	}

	protected Button createContainerButton() {
		if (!Settings.enableContainerOptionOnPlayerShop || !(shopkeeper.getType() instanceof PlayerShopType)) {
			return null;
		}
		return new Button() {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createContainerButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Also triggers save:
				getUISession(player).closeDelayedAndRunTask(() -> {
					// Open the shop container inventory:
					if (!player.isValid() || !shopkeeper.isValid()) return;
					((PlayerShopkeeper) shopkeeper).openContainerWindow(player);
				});
			}
		};
	}

	protected abstract List<TradingRecipeDraft> getTradingRecipes();

	@Override
	protected void saveRecipes(Session session) {
		Player player = session.getPlayer();
		this.clearRecipes();
		for (TradingRecipeDraft recipe : session.getRecipes()) {
			if (!recipe.isValid()) {
				this.handleInvalidRecipeDraft(player, recipe);
				continue;
			}
			this.addRecipe(player, recipe);
		}
	}

	// Called for every recipe draft that is not valid:
	protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
	}

	protected abstract void clearRecipes();

	protected abstract void addRecipe(Player player, TradingRecipeDraft recipe);
}
