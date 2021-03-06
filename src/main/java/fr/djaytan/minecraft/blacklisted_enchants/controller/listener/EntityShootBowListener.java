/*
 * Blacklisted enchants plugin for Minecraft (Bukkit servers)
 * Copyright (C) 2022 - Loïc DUBOIS-TERMOZ
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.djaytan.minecraft.blacklisted_enchants.controller.listener;

import fr.djaytan.minecraft.blacklisted_enchants.controller.api.EnchantmentController;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class EntityShootBowListener implements Listener {

  private final EnchantmentController enchantmentController;

  @Inject
  public EntityShootBowListener(@NotNull EnchantmentController enchantmentController) {
    this.enchantmentController = enchantmentController;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEntityShootBow(@NotNull EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    event.setConsumeItem(true);

    Set<Enchantment> removedBlacklistedEnchantments =
        enchantmentController.removeBlacklistedEnchantments(event.getBow());

    if (removedBlacklistedEnchantments.isEmpty()) {
      return;
    }

    enchantmentController.sendRemovedBlacklistedEnchantmentsMessage(
        player, removedBlacklistedEnchantments);
  }
}
