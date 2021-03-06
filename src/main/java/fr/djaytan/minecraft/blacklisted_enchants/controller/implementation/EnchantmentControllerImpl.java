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

package fr.djaytan.minecraft.blacklisted_enchants.controller.implementation;

import com.google.common.base.Preconditions;
import fr.djaytan.minecraft.blacklisted_enchants.RemakeBukkitLogger;
import fr.djaytan.minecraft.blacklisted_enchants.controller.api.EnchantmentController;
import fr.djaytan.minecraft.blacklisted_enchants.controller.api.MessageController;
import fr.djaytan.minecraft.blacklisted_enchants.model.config.data.PluginConfig;
import fr.djaytan.minecraft.blacklisted_enchants.view.message.EnchantsMessage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class EnchantmentControllerImpl implements EnchantmentController {

  private static final Enchantment FALLBACK_ENCHANTMENT = Enchantment.DURABILITY;

  private final EnchantsMessage enchantsMessage;
  private final MessageController messageController;
  private final RemakeBukkitLogger logger;
  private final PluginConfig pluginConfig;

  @Inject
  public EnchantmentControllerImpl(
      @NotNull EnchantsMessage enchantsMessage,
      @NotNull MessageController messageController,
      @NotNull RemakeBukkitLogger logger,
      @NotNull PluginConfig pluginConfig) {
    this.enchantsMessage = enchantsMessage;
    this.messageController = messageController;
    this.logger = logger;
    this.pluginConfig = pluginConfig;
  }

  @Override
  public void adjustEnchantments(@Nullable ItemStack itemStack) {
    Set<Enchantment> removedBlacklistedEnchants = removeBlacklistedEnchantments(itemStack);

    if (removedBlacklistedEnchants.isEmpty()) {
      return;
    }

    addFallbackEnchantmentIfEmpty(itemStack);
  }

  @Override
  public @NotNull Set<Enchantment> removeBlacklistedEnchantments(@Nullable ItemStack itemStack) {
    if (itemStack == null
        || itemStack.getType() == Material.AIR
        || itemStack.getAmount() <= 0
        || !itemStack.hasItemMeta()) {
      return Collections.emptySet();
    }

    ItemMeta itemMeta = itemStack.getItemMeta();
    List<Enchantment> blacklistedEnchantments = pluginConfig.getBlacklistedEnchantments();
    Set<Enchantment> detectedBlacklistedEnchantments = new HashSet<>();

    for (Enchantment enchantment : itemMeta.getEnchants().keySet()) {
      if (!blacklistedEnchantments.contains(enchantment)) {
        continue;
      }

      itemMeta.removeEnchant(enchantment);
      detectedBlacklistedEnchantments.add(enchantment);
    }

    if (itemMeta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
      for (Enchantment enchantment : enchantmentStorageMeta.getStoredEnchants().keySet()) {
        if (!blacklistedEnchantments.contains(enchantment)) {
          continue;
        }

        enchantmentStorageMeta.removeStoredEnchant(enchantment);
        detectedBlacklistedEnchantments.add(enchantment);
      }
    }

    itemStack.setItemMeta(itemMeta);

    if (!detectedBlacklistedEnchantments.isEmpty()) {
      logger.debug(
          "Enchantment(s) {} removed from an item.",
          detectedBlacklistedEnchantments.stream()
              .map(Enchantment::getKey)
              .map(NamespacedKey::getKey)
              .map(String::toUpperCase)
              .collect(Collectors.toSet()));
    }

    return detectedBlacklistedEnchantments;
  }

  @Override
  public void removeBlacklistedEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
    Preconditions.checkNotNull(enchantments);

    Set<Enchantment> detectedBlacklistedEnchantments = new HashSet<>();

    for (Enchantment enchantment : enchantments.keySet()) {
      if (!pluginConfig.getBlacklistedEnchantments().contains(enchantment)) {
        continue;
      }

      enchantments.remove(enchantment);
      detectedBlacklistedEnchantments.add(enchantment);
    }

    if (!detectedBlacklistedEnchantments.isEmpty()) {
      logger.debug(
          "Enchantment(s) {} removed at enchant time.",
          detectedBlacklistedEnchantments.stream()
              .map(Enchantment::getKey)
              .map(NamespacedKey::getKey)
              .map(String::toUpperCase)
              .collect(Collectors.toSet()));
    }
  }

  @Override
  public void addFallbackEnchantmentIfEmpty(@Nullable ItemStack itemStack) {
    if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0) {
      return;
    }

    ItemMeta itemMeta = itemStack.getItemMeta();

    if (itemMeta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
      if (enchantmentStorageMeta.hasStoredEnchants()) {
        return;
      }

      enchantmentStorageMeta.addStoredEnchant(
          FALLBACK_ENCHANTMENT, FALLBACK_ENCHANTMENT.getMaxLevel(), false);
      itemStack.setItemMeta(enchantmentStorageMeta);

      return;
    }

    if (itemMeta.hasEnchants()) {
      return;
    }

    itemMeta.addEnchant(FALLBACK_ENCHANTMENT, FALLBACK_ENCHANTMENT.getMaxLevel(), false);
    itemStack.setItemMeta(itemMeta);
  }

  @Override
  public void addFallbackEnchantmentIfEmpty(@NotNull Map<Enchantment, Integer> enchantments) {
    Preconditions.checkNotNull(enchantments);

    if (enchantments.isEmpty()) {
      enchantments.put(FALLBACK_ENCHANTMENT, FALLBACK_ENCHANTMENT.getMaxLevel());
    }
  }

  @Override
  public void applyFallbackEnchantmentOffer(@NotNull EnchantmentOffer enchantmentOffer) {
    Preconditions.checkNotNull(enchantmentOffer);

    Enchantment blacklistedEnchantment = enchantmentOffer.getEnchantment();
    int blacklistedEnchantmentLevel = enchantmentOffer.getEnchantmentLevel();
    Enchantment fallbackEnchantment = FALLBACK_ENCHANTMENT;

    enchantmentOffer.setEnchantment(fallbackEnchantment);
    enchantmentOffer.setEnchantmentLevel(fallbackEnchantment.getMaxLevel());

    logger.debug(
        "Blacklisted enchantment offer '{}' (level {}) replaced by '{}' (level {})",
        blacklistedEnchantment.getKey().getKey(),
        blacklistedEnchantmentLevel,
        enchantmentOffer.getEnchantment().getKey().getKey(),
        enchantmentOffer.getEnchantmentLevel());
  }

  @Override
  public boolean isBlacklistedEnchantment(@NotNull Enchantment enchantment) {
    Preconditions.checkNotNull(enchantment);
    return pluginConfig.getBlacklistedEnchantments().contains(enchantment);
  }

  @Override
  public void sendRemovedBlacklistedEnchantmentsMessage(
      @NotNull Audience audience, @NotNull Set<Enchantment> removedBlacklistedEnchantments) {
    Preconditions.checkNotNull(audience);
    Preconditions.checkNotNull(removedBlacklistedEnchantments);
    Preconditions.checkArgument(
        !removedBlacklistedEnchantments.isEmpty(), "The set mustn't be empty.");

    messageController.sendWarningMessage(
        audience, enchantsMessage.removedBlacklistedEnchants(removedBlacklistedEnchantments));
  }
}
