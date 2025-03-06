package com.nisovin.shopkeepers.ui;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerOpenUIEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperOpenUIEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;
import com.nisovin.shopkeepers.ui.lib.AbstractUIType;
import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.ui.lib.UISessionManager.SessionHandler;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.ui.lib.ViewProvider;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKUIRegistry extends AbstractTypeRegistry<AbstractUIType>
		implements UIRegistry<AbstractUIType> {

	private static final SessionHandler UI_SESSION_HANDLER = new SessionHandler() {
		@Override
		public PlayerOpenUIEvent createPlayerOpenUIEvent(
				ViewProvider viewProvider,
				Player player,
				boolean silentRequest,
				UIState uiState
		) {
			if (viewProvider instanceof ShopkeeperViewProvider) {
				var shopkeeper = ((ShopkeeperViewProvider) viewProvider).getShopkeeper();
				return new ShopkeeperOpenUIEvent(
						shopkeeper,
						viewProvider.getUIType(),
						player,
						silentRequest
				);
			} else {
				return SessionHandler.super.createPlayerOpenUIEvent(
						viewProvider,
						player,
						silentRequest,
						uiState
				);
			}
		}
	};

	private final ShopkeepersPlugin plugin;

	public SKUIRegistry(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		UISessionManager.enable(plugin, UI_SESSION_HANDLER);
	}

	public void onDisable() {
		UISessionManager.disable();
	}

	@Override
	protected String getTypeName() {
		return "UI type";
	}

	@Override
	public Collection<? extends View> getUISessions() {
		return UISessionManager.getInstance().getUISessions();
	}

	@Override
	public Collection<? extends View> getUISessions(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		return UISessionManager.getInstance().getUISessionsForContext(shopkeeper);
	}

	@Override
	public Collection<? extends View> getUISessions(Shopkeeper shopkeeper, UIType uiType) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		return UISessionManager.getInstance().getUISessionsForContext(shopkeeper, uiType);
	}

	@Override
	public Collection<? extends View> getUISessions(UIType uiType) {
		return UISessionManager.getInstance().getUISessions(uiType);
	}

	@Override
	public @Nullable View getUISession(Player player) {
		return UISessionManager.getInstance().getUISession(player);
	}

	@Override
	public void abortUISessions() {
		UISessionManager.getInstance().abortUISessions();
	}

	@Override
	public void abortUISessions(Shopkeeper shopkeeper) {
		UISessionManager.getInstance().abortUISessionsForContext(shopkeeper);
	}

	@Override
	public void abortUISessionsDelayed(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		UISessionManager.getInstance().abortUISessionsForContextDelayed(shopkeeper);
	}
}
