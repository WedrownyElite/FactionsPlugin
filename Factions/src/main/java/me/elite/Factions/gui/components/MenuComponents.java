package me.elite.Factions.gui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

/**
 * Utility class for creating common menu components
 */
public final class MenuComponents {

    /**
     * Create a "Back" button with left arrow player head
     */
    public static ItemStack createBackButton() {
        ItemStack backButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) backButton.getItemMeta();

        skullMeta.setDisplayName(ChatColor.GRAY + "← Back");
        skullMeta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Click to go back"));

        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=";
        setCustomTexture(skullMeta, texture);

        backButton.setItemMeta(skullMeta);
        return backButton;
    }

    /**
     * Create a custom player head with base64 texture
     */
    public static ItemStack createCustomHead(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        setCustomTexture(skullMeta, texture);
        head.setItemMeta(skullMeta);
        return head;
    }

    /**
     * Create black glass pane filler
     */
    public static ItemStack createGlassPaneFiller() {
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);
        return blackGlass;
    }

    private static void setCustomTexture(SkullMeta skullMeta, String texture) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            // Ignore texture setting failures
        }
    }

    private MenuComponents() {
        // Prevent instantiation
    }
}