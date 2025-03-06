package com.nisovin.shopkeepers.ui.villager.equipmentEditor;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.util.java.Validate;

public class VillagerEquipmentEditorUI {

	public static boolean request(AbstractVillager entity, Player player) {
		Validate.notNull(entity, "entity is null");
		Validate.notNull(player, "player is null");

		var viewProvider = new VillagerEquipmentEditorViewProvider(entity);
		return UISessionManager.getInstance().requestUI(viewProvider, player);
	}

	private VillagerEquipmentEditorUI() {
	}
}
