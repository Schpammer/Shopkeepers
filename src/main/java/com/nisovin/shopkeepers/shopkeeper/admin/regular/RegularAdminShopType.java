package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopType;

public class RegularAdminShopType extends AbstractAdminShopType<SKRegularAdminShopkeeper> {

	public RegularAdminShopType() {
		super("admin", ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeAdminRegular;
	}

	@Override
	public String getDescription() {
		return Messages.shopTypeDescAdminRegular;
	}

	@Override
	public String getSetupDescription() {
		return Messages.shopSetupDescAdminRegular;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Messages.tradeSetupDescAdminRegular;
	}

	@Override
	public SKRegularAdminShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKRegularAdminShopkeeper shopkeeper = new SKRegularAdminShopkeeper(id, shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKRegularAdminShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SKRegularAdminShopkeeper shopkeeper = new SKRegularAdminShopkeeper(id, configSection);
		return shopkeeper;
	}
}
