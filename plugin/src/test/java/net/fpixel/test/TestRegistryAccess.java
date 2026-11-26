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
                case "createItemStack" -> allocateItemStack(nsKey);
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

    private static ItemStack allocateItemStack(NamespacedKey nsKey) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            ItemStack stack = (ItemStack) unsafe.allocateInstance(ItemStack.class);
            Material mat = Material.matchMaterial(nsKey.getKey());
            if (mat != null) {
                Field typeField = ItemStack.class.getDeclaredField("type");
                typeField.setAccessible(true);
                typeField.set(stack, mat);
            }
            Field amountField = ItemStack.class.getDeclaredField("amount");
            amountField.setAccessible(true);
            amountField.setInt(stack, 1);
            return stack;
        } catch (Exception e) {
            return null;
        }
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
