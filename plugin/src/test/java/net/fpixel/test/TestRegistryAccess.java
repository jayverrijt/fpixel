package net.fpixel.test;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Minimal ServiceLoader implementation of Paper's {@link RegistryAccess} for unit tests.
 */
public class TestRegistryAccess implements RegistryAccess {

    private final Map<String, Registry<?>> registries = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private <T extends Keyed> Registry<T> registryFor(Class<?> elementType) {
        Registry<?> existing = registries.get(elementType.getName());
        if (existing != null) return (Registry<T>) existing;

        Registry<?> created = BukkitTestHarness.proxy(Registry.class, (m, args) -> switch (m.getName()) {
            case "get", "getOrThrow" -> createElement(elementType, (Key) args[0]);
            case "stream", "keyStream" -> Stream.empty();
            case "iterator" -> Collections.emptyIterator();
            case "size" -> 0;
            case "getKey" -> ((Keyed) args[0]).getKey();
            default -> null;
        });
        registries.putIfAbsent(elementType.getName(), created);
        return (Registry<T>) registries.get(elementType.getName());
    }

    private Object createElement(Class<?> elementType, Key key) {
        NamespacedKey nsKey = (key instanceof NamespacedKey nk)
                ? nk
                : new NamespacedKey(key.namespace(), key.value());

        if (elementType == ItemType.class) {
            return BukkitTestHarness.proxy(ItemType.class, (m, args) -> switch (m.getName()) {
                case "getKey", "key" -> nsKey;
                case "createItemStack" -> allocateItemStack(nsKey, args.length > 0 && args[0] instanceof Integer i ? i : 1);
                case "asMaterial" -> Material.matchMaterial(nsKey.getKey());
                default -> null;
            });
        }

        Class<?> iface = elementType.isInterface() ? elementType : Keyed.class;
        return BukkitTestHarness.proxy(iface, (m, args) -> switch (m.getName()) {
            case "getKey", "key" -> nsKey;
            default -> null;
        });
    }

    private static class StubItemStack extends ItemStack {
        private Material stubType;
        private int stubAmount;
        private ItemMeta stubMeta;

        @Override public @NotNull Material getType() { return stubType != null ? stubType : Material.AIR; }
        @Override public void setType(@NotNull Material type) { this.stubType = type; }
        @Override public int getAmount() { return stubAmount; }
        @Override public void setAmount(int amount) { this.stubAmount = amount; }
        @Override public ItemMeta getItemMeta() {
            if (stubMeta == null) {
                stubMeta = BukkitTestHarness.proxy(ItemMeta.class, (m, args) -> switch (m.getName()) {
                    case "hasDisplayName", "hasLore", "hasEnchants", "isUnbreakable" -> false;
                    case "clone" -> stubMeta;
                    default -> m.getReturnType() == boolean.class ? false : null;
                });
            }
            return stubMeta;
        }
        @Override public boolean setItemMeta(ItemMeta itemMeta) { this.stubMeta = itemMeta; return true; }
        @Override public boolean hasItemMeta() { return stubMeta != null; }
        @Override public @NotNull ItemStack clone() {
            StubItemStack copy = allocateStub(stubType, stubAmount);
            copy.stubMeta = this.stubMeta;
            return copy;
        }
    }

    private static StubItemStack allocateStub(Material mat, int amount) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            StubItemStack stack = (StubItemStack) unsafe.allocateInstance(StubItemStack.class);
            stack.stubType = mat;
            stack.stubAmount = amount;
            return stack;
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack allocateItemStack(NamespacedKey nsKey, int amount) {
        Material mat = Material.matchMaterial(nsKey.getKey());
        return allocateStub(mat != null ? mat : Material.STONE, amount);
    }

    @Override
    @SuppressWarnings({"removal"})
    public @NotNull <T extends Keyed> Registry<T> getRegistry(@NotNull Class<T> type) {
        return registryFor(type);
    }

    @Override
    public @NotNull <T extends Keyed> Registry<T> getRegistry(@NotNull RegistryKey<T> registryKey) {
        if (registryKey == RegistryKey.ITEM) return registryFor(ItemType.class);
        if (registryKey == RegistryKey.DAMAGE_TYPE) return registryFor(DamageType.class);
        if (registryKey == RegistryKey.SOUND_EVENT) return registryFor(org.bukkit.Sound.class);
        return registryFor(Keyed.class);
    }
}
