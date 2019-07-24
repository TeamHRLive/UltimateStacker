package com.songoda.ultimatestacker.utils;

import com.songoda.lootables.loot.Drop;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.entity.Check;
import com.songoda.ultimatestacker.entity.EntityStack;
import com.songoda.ultimatestacker.utils.settings.Setting;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Methods {

    public static void updateInventory(Item item, Inventory inventory) {
        int amount = Methods.getActualItemAmount(item);

        while (amount > 0) {
            int subtract = Math.min(amount, 64);
            amount -= subtract;
            ItemStack newItem = item.getItemStack().clone();
            newItem.setAmount(subtract);
            Map<Integer, ItemStack> result = inventory.addItem(newItem);
            if (result.get(0) != null) {
                amount += result.get(0).getAmount();
                break;
            }
        }

        if (amount <= 0)
            item.remove();
        else
            Methods.updateItemAmount(item, amount);
    }

    public static void updateItemAmount(Item item, int newAmount) {
        UltimateStacker plugin = UltimateStacker.getInstance();
        Material material = item.getItemStack().getType();
        String name = Methods.convertToInvisibleString("IS") +
                compileItemName(item.getItemStack(), newAmount);

        if (newAmount > 32) {
            item.setMetadata("US_AMT", new FixedMetadataValue(plugin, newAmount));
            item.getItemStack().setAmount(32);
        } else {
            item.removeMetadata("US_AMT", plugin);
            item.getItemStack().setAmount(newAmount);
        }

        if (plugin.getItemFile().getConfig().getBoolean("Items." + material + ".Has Hologram")
                && Setting.ITEM_HOLOGRAMS.getBoolean()) {
            if (newAmount == 1 && !Setting.ITEM_HOLOGRAM_SINGLE.getBoolean()) return;
            item.setCustomName(name);
            item.setCustomNameVisible(true);
        }
    }

    public static int getActualItemAmount(Item item) {
        if (item.hasMetadata("US_AMT")) {
            return item.getMetadata("US_AMT").get(0).asInt();
        } else {
            return item.getItemStack().getAmount();
        }
    }

    public static String compileItemName(ItemStack item, int amount) {
        String nameFormat = Setting.NAME_FORMAT_ITEM.getString();
        String displayName = Methods.formatText(UltimateStacker.getInstance().getItemFile().getConfig()
                .getString("Items." + item.getType().name() + ".Display Name"));

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            displayName = Setting.NAME_FORMAT_RESET.getBoolean() ?
                    ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getItemMeta().getDisplayName();

        nameFormat = nameFormat.replace("{TYPE}", displayName);
        nameFormat = nameFormat.replace("{AMT}", Integer.toString(amount));

        if (amount == 1 && !Setting.SHOW_STACK_SIZE_SINGLE.getBoolean()) {
            nameFormat = nameFormat.replaceAll("\\[.*?]", "");
        } else {
            nameFormat = nameFormat.replace("[", "").replace("]", "");
        }

        String info = Methods.convertToInvisibleString(Methods.insertSemicolon(String.valueOf(amount)) + ":");

        return info + Methods.formatText(nameFormat).trim();
    }

    public static boolean canFly(LivingEntity entity) {
        switch (entity.getType()) {
            case GHAST:
            case BLAZE:
            case PHANTOM:
            case BAT:
                return true;
            default:
                return false;
        }
    }

    public static void processDrop(LivingEntity entity, Drop drop) {
        if (drop == null) return;

        if (drop.getItemStack() != null)
            entity.getWorld().dropItemNaturally(entity.getLocation(), drop.getItemStack());
        if (drop.getCommand() != null) {
            String command = drop.getCommand();
            if (entity.getKiller() != null) {
                command = command.replace("%player%", entity.getKiller().getName());
            }
            if (!command.contains("%player%"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    public static LivingEntity newEntity(LivingEntity toClone) {
        LivingEntity newEntity = (LivingEntity) toClone.getWorld().spawnEntity(toClone.getLocation(), toClone.getType());
        newEntity.setVelocity(toClone.getVelocity());

        List<String> checks = Setting.STACK_CHECKS.getStringList();

        for (String checkStr : checks) {
            Check check = Check.valueOf(checkStr);
            switch (check) {
                case AGE: {
                    if (!(toClone instanceof Ageable) || ((Ageable) toClone).isAdult()) break;
                    ((Ageable) newEntity).setBaby();
                    break;
                }
                case NERFED: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_9)) break;
                    if (!toClone.hasAI()) newEntity.setAI(false);
                }
                case IS_TAMED: {
                    if (!(toClone instanceof Tameable)) break;
                    ((Tameable) newEntity).setTamed(((Tameable) toClone).isTamed());
                }
                case ANIMAL_OWNER: {
                    if (!(toClone instanceof Tameable)) break;
                    ((Tameable) newEntity).setOwner(((Tameable) toClone).getOwner());
                }
                case SKELETON_TYPE: {
                    if (!(toClone instanceof Skeleton)
                            || UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_12)) break;
                    ((Skeleton) newEntity).setSkeletonType(((Skeleton) toClone).getSkeletonType());
                    break;
                }
                case SHEEP_COLOR: {
                    if (!(toClone instanceof Sheep)) break;
                    ((Sheep) newEntity).setColor(((Sheep) toClone).getColor());
                    break;
                }
                case SHEEP_SHEERED: {
                    if (!(toClone instanceof Sheep)) break;
                    ((Sheep) newEntity).setSheared(((Sheep) toClone).isSheared());
                    break;
                }
                case LLAMA_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)
                            || !(toClone instanceof Llama)) break;
                    ((Llama) newEntity).setColor(((Llama) toClone).getColor());
                    break;
                }
                case LLAMA_STRENGTH: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)
                            || !(toClone instanceof Llama)) break;
                    ((Llama) newEntity).setStrength(((Llama) toClone).getStrength());
                    break;
                }
                case VILLAGER_PROFESSION: {
                    if (!(toClone instanceof Villager)) break;
                    ((Villager) newEntity).setProfession(((Villager) toClone).getProfession());
                    break;
                }
                case SLIME_SIZE: {
                    if (!(toClone instanceof Slime)) break;
                    ((Slime) newEntity).setSize(((Slime) toClone).getSize());
                    break;
                }
                case HORSE_JUMP: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)
                            || !(toClone instanceof AbstractHorse)) break;
                    ((AbstractHorse) newEntity).setJumpStrength(((AbstractHorse) toClone).getJumpStrength());
                    break;
                }
                case HORSE_COLOR: {
                    if (!(toClone instanceof Horse)) break;
                    ((Horse) newEntity).setColor(((Horse) toClone).getColor());
                    break;
                }
                case HORSE_STYLE: {
                    if (!(toClone instanceof Horse)) break;
                    ((Horse) newEntity).setStyle(((Horse) toClone).getStyle());
                    break;
                }
                case ZOMBIE_BABY: {
                    if (!(toClone instanceof Zombie)) break;
                    ((Zombie) newEntity).setBaby(((Zombie) toClone).isBaby());
                    break;
                }
                case WOLF_COLLAR_COLOR: {
                    if (!(toClone instanceof Wolf)) break;
                    ((Wolf) newEntity).setCollarColor(((Wolf) toClone).getCollarColor());
                    break;
                }
                case OCELOT_TYPE: {
                    if (!(toClone instanceof Ocelot)) break;
                    ((Ocelot) newEntity).setCatType(((Ocelot) toClone).getCatType());
                }
                case CAT_TYPE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_14)
                            || !(toClone instanceof Cat)) break;
                    ((Cat) newEntity).setCatType(((Cat) toClone).getCatType());
                    break;
                }
                case RABBIT_TYPE: {
                    if (!(toClone instanceof Rabbit)) break;
                    ((Rabbit) newEntity).setRabbitType(((Rabbit) toClone).getRabbitType());
                    break;
                }
                case PARROT_TYPE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_12)
                            || !(toClone instanceof Parrot)) break;
                    ((Parrot) newEntity).setVariant(((Parrot) toClone).getVariant());
                    break;
                }
                case PUFFERFISH_STATE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(toClone instanceof PufferFish)) break;
                    ((PufferFish) newEntity).setPuffState(((PufferFish) toClone).getPuffState());
                    break;
                }
                case TROPICALFISH_PATTERN: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(toClone instanceof TropicalFish)) break;
                    ((TropicalFish) newEntity).setPattern(((TropicalFish) toClone).getPattern());
                    break;
                }
                case TROPICALFISH_PATTERN_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(toClone instanceof TropicalFish)) break;
                    ((TropicalFish) newEntity).setPatternColor(((TropicalFish) toClone).getPatternColor());
                    break;
                }
                case TROPICALFISH_BODY_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(toClone instanceof TropicalFish)) break;
                    ((TropicalFish) newEntity).setBodyColor(((TropicalFish) toClone).getBodyColor());
                    break;
                }
                case PHANTOM_SIZE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(toClone instanceof Phantom)) break;
                    ((Phantom) newEntity).setSize(((Phantom) toClone).getSize());
                    break;
                }
            }
        }

        if (Setting.KEEP_FIRE.getBoolean())
            newEntity.setFireTicks(toClone.getFireTicks());
        if (Setting.KEEP_POTION.getBoolean())
            newEntity.addPotionEffects(toClone.getActivePotionEffects());

        return newEntity;
    }

    public static List<LivingEntity> getSimilarEntitiesAroundEntity(LivingEntity initalEntity) {

        int searchRadius = Setting.SEARCH_RADIUS.getInt();

        //Create a list of all entities around the initial entity of the same type.
        List<LivingEntity> entityList = initalEntity.getNearbyEntities(searchRadius, searchRadius, searchRadius).stream()
                .filter(entity -> entity.getType() == initalEntity.getType() && entity != initalEntity)
                .map(entity -> (LivingEntity) entity).collect(Collectors.toList());

        List<String> checks = Setting.STACK_CHECKS.getStringList();

        if (Setting.ONLY_STACK_FLYING_DOWN.getBoolean() && Methods.canFly(initalEntity))
            entityList.removeIf(entity -> entity.getLocation().getY() > initalEntity.getLocation().getY());

        for (String checkStr : checks) {
            Check check = Check.valueOf(checkStr);
            switch (check) {
                case SPAWN_REASON: {
                    if (initalEntity.hasMetadata("US_REASON"))
                        entityList.removeIf(entity -> entity.hasMetadata("US_REASON") && !entity.getMetadata("US_REASON").get(0).asString().equals("US_REASON"));
                }
                case AGE: {
                    if (!(initalEntity instanceof Ageable)) break;

                    if (((Ageable) initalEntity).isAdult()) {
                        entityList.removeIf(entity -> !((Ageable) entity).isAdult());
                    } else {
                        entityList.removeIf(entity -> ((Ageable) entity).isAdult());
                    }
                    break;
                }
                case NERFED: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_9)) break;
                    entityList.removeIf(entity -> entity.hasAI() != initalEntity.hasAI());
                }
                case IS_TAMED: {
                    if (!(initalEntity instanceof Tameable)) break;
                    entityList.removeIf(entity -> ((Tameable) entity).isTamed());
                }
                case ANIMAL_OWNER: {
                    if (!(initalEntity instanceof Tameable)) break;

                    Tameable tameable = ((Tameable) initalEntity);
                    entityList.removeIf(entity -> ((Tameable) entity).getOwner() != tameable.getOwner());
                }
                case PIG_SADDLE: {
                    if (!(initalEntity instanceof Pig)) break;
                    entityList.removeIf(entity -> ((Pig) entity).hasSaddle());
                    break;
                }
                case SKELETON_TYPE: {
                    if (!(initalEntity instanceof Skeleton)) break;

                    Skeleton skeleton = (Skeleton) initalEntity;
                    entityList.removeIf(entity -> ((Skeleton) entity).getSkeletonType() != skeleton.getSkeletonType());
                    break;
                }
                case SHEEP_COLOR: {
                    if (!(initalEntity instanceof Sheep)) break;

                    Sheep sheep = ((Sheep) initalEntity);
                    entityList.removeIf(entity -> ((Sheep) entity).getColor() != sheep.getColor());
                    break;
                }
                case SHEEP_SHEERED: {
                    if (!(initalEntity instanceof Sheep)) break;

                    Sheep sheep = ((Sheep) initalEntity);
                    if (sheep.isSheared()) {
                        entityList.removeIf(entity -> !((Sheep) entity).isSheared());
                    } else {
                        entityList.removeIf(entity -> ((Sheep) entity).isSheared());
                    }
                    break;
                }
                case LLAMA_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)
                            || !(initalEntity instanceof Llama)) break;
                    Llama llama = ((Llama) initalEntity);
                    entityList.removeIf(entity -> ((Llama) entity).getColor() != llama.getColor());
                    break;
                }
                case LLAMA_STRENGTH: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)
                            || !(initalEntity instanceof Llama)) break;
                    Llama llama = ((Llama) initalEntity);
                    entityList.removeIf(entity -> ((Llama) entity).getStrength() != llama.getStrength());
                    break;
                }
                case VILLAGER_PROFESSION: {
                    if (!(initalEntity instanceof Villager)) break;
                    Villager villager = ((Villager) initalEntity);
                    entityList.removeIf(entity -> ((Villager) entity).getProfession() != villager.getProfession());
                    break;
                }
                case SLIME_SIZE: {
                    if (!(initalEntity instanceof Slime)) break;
                    Slime slime = ((Slime) initalEntity);
                    entityList.removeIf(entity -> ((Slime) entity).getSize() != slime.getSize());
                    break;
                }
                case HORSE_CARRYING_CHEST: {
                    if (UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)) {
                        if (!(initalEntity instanceof ChestedHorse)) break;
                        entityList.removeIf(entity -> ((ChestedHorse) entity).isCarryingChest());
                    } else {
                        if (!(initalEntity instanceof Horse)) break;
                        entityList.removeIf(entity -> ((Horse) entity).isCarryingChest());
                    }
                    break;
                }
                case HORSE_HAS_ARMOR: {
                    if (!(initalEntity instanceof Horse)) break;
                    entityList.removeIf(entity -> ((Horse) entity).getInventory().getArmor() != null);
                    break;
                }
                case HORSE_HAS_SADDLE: {
                    if (UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            && initalEntity instanceof AbstractHorse) {
                        entityList.removeIf(entity -> ((AbstractHorse) entity).getInventory().getSaddle() != null);
                        break;
                    }
                    if (!(initalEntity instanceof Horse)) break;
                    entityList.removeIf(entity -> ((Horse) entity).getInventory().getSaddle() != null);
                    break;
                }
                case HORSE_JUMP: {
                    if (UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_11)) {
                        if (!(initalEntity instanceof AbstractHorse)) break;
                        AbstractHorse horse = ((AbstractHorse) initalEntity);
                        entityList.removeIf(entity -> ((AbstractHorse) entity).getJumpStrength() != horse.getJumpStrength());
                    } else {
                        if (!(initalEntity instanceof Horse)) break;
                        Horse horse = ((Horse) initalEntity);
                        entityList.removeIf(entity -> ((Horse) entity).getJumpStrength() != horse.getJumpStrength());

                    }
                    break;
                }
                case HORSE_COLOR: {
                    if (!(initalEntity instanceof Horse)) break;
                    Horse horse = ((Horse) initalEntity);
                    entityList.removeIf(entity -> ((Horse) entity).getColor() != horse.getColor());
                    break;
                }
                case HORSE_STYLE: {
                    if (!(initalEntity instanceof Horse)) break;
                    Horse horse = ((Horse) initalEntity);
                    entityList.removeIf(entity -> ((Horse) entity).getStyle() != horse.getStyle());
                    break;
                }
                case ZOMBIE_BABY: {
                    if (!(initalEntity instanceof Zombie)) break;
                    Zombie zombie = (Zombie) initalEntity;
                    entityList.removeIf(entity -> ((Zombie) entity).isBaby() != zombie.isBaby());
                    break;
                }
                case WOLF_COLLAR_COLOR: {
                    if (!(initalEntity instanceof Wolf)) break;
                    Wolf wolf = (Wolf) initalEntity;
                    entityList.removeIf(entity -> ((Wolf) entity).getCollarColor() != wolf.getCollarColor());
                    break;
                }
                case OCELOT_TYPE: {
                    if (!(initalEntity instanceof Ocelot)) break;
                    Ocelot ocelot = (Ocelot) initalEntity;
                    entityList.removeIf(entity -> ((Ocelot) entity).getCatType() != ocelot.getCatType());
                }
                case CAT_TYPE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_14)
                            || !(initalEntity instanceof Cat)) break;
                    Cat cat = (Cat) initalEntity;
                    entityList.removeIf(entity -> ((Cat) entity).getCatType() != cat.getCatType());
                    break;
                }
                case RABBIT_TYPE: {
                    if (!(initalEntity instanceof Rabbit)) break;
                    Rabbit rabbit = (Rabbit) initalEntity;
                    entityList.removeIf(entity -> ((Rabbit) entity).getRabbitType() != rabbit.getRabbitType());
                    break;
                }
                case PARROT_TYPE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_12)
                            || !(initalEntity instanceof Parrot)) break;
                    Parrot parrot = (Parrot) initalEntity;
                    entityList.removeIf(entity -> ((Parrot) entity).getVariant() != parrot.getVariant());
                    break;
                }
                case PUFFERFISH_STATE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(initalEntity instanceof PufferFish)) break;
                    PufferFish pufferFish = (PufferFish) initalEntity;
                    entityList.removeIf(entity -> ((PufferFish) entity).getPuffState() != pufferFish.getPuffState());
                    break;
                }
                case TROPICALFISH_PATTERN: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(initalEntity instanceof TropicalFish)) break;
                    TropicalFish tropicalFish = (TropicalFish) initalEntity;
                    entityList.removeIf(entity -> ((TropicalFish) entity).getPattern() != tropicalFish.getPattern());
                    break;
                }
                case TROPICALFISH_PATTERN_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(initalEntity instanceof TropicalFish)) break;
                    TropicalFish tropicalFish = (TropicalFish) initalEntity;
                    entityList.removeIf(entity -> ((TropicalFish) entity).getPatternColor() != tropicalFish.getPatternColor());
                    break;
                }
                case TROPICALFISH_BODY_COLOR: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(initalEntity instanceof TropicalFish)) break;
                    TropicalFish tropicalFish = (TropicalFish) initalEntity;
                    entityList.removeIf(entity -> ((TropicalFish) entity).getBodyColor() != tropicalFish.getBodyColor());
                    break;
                }
                case PHANTOM_SIZE: {
                    if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)
                            || !(initalEntity instanceof Phantom)) break;
                    Phantom phantom = (Phantom) initalEntity;
                    entityList.removeIf(entity -> ((Phantom) entity).getSize() != phantom.getSize());
                    break;
                }
            }
        }

        if (initalEntity.hasMetadata("breedCooldown")) {
            entityList.removeIf(entity -> !entity.hasMetadata("breedCooldown"));
        }

        return entityList;
    }

    public static void splitFromStack(LivingEntity entity) {
        UltimateStacker instance = UltimateStacker.getInstance();
        EntityStack stack = instance.getEntityStackManager().getStack(entity);

        if (stack.getAmount() <= 1) return;

        LivingEntity newEntity = Methods.newEntity(entity);

        int newAmount = stack.getAmount() - 1;
        if (newAmount != 1)
            instance.getEntityStackManager().addStack(new EntityStack(newEntity, newAmount));
        stack.setAmount(1);
        instance.getEntityStackManager().removeStack(entity);
        entity.setVelocity(getRandomVector());
    }


    private static Vector getRandomVector() {
        return new Vector(ThreadLocalRandom.current().nextDouble(-1, 1.01), 0, ThreadLocalRandom.current().nextDouble(-1, 1.01)).normalize().multiply(0.5);
    }

    public static String compileSpawnerName(EntityType entityType, int amount) {
        String nameFormat = UltimateStacker.getInstance().getConfig().getString("Spawners.Name Format");
        String displayName = Methods.formatText(UltimateStacker.getInstance().getSpawnerFile().getConfig().getString("Spawners." + entityType.name() + ".Display Name"));

        nameFormat = nameFormat.replace("{TYPE}", displayName);
        nameFormat = nameFormat.replace("{AMT}", Integer.toString(amount));

        String info = Methods.convertToInvisibleString(insertSemicolon(String.valueOf(amount)) + ":");
        return info + Methods.formatText(nameFormat).trim();
    }

    public static String compileEntityName(Entity entity, int amount) {
        String nameFormat = Setting.NAME_FORMAT_ENTITY.getString();
        String displayName = Methods.formatText(UltimateStacker.getInstance().getMobFile().getConfig().getString("Mobs." + entity.getType().name() + ".Display Name"));

        nameFormat = nameFormat.replace("{TYPE}", displayName);
        nameFormat = nameFormat.replace("{AMT}", Integer.toString(amount));

        String info = Methods.convertToInvisibleString(insertSemicolon(String.valueOf(amount)) + ":");

        return info + Methods.formatText(nameFormat).trim();
    }

    public static void takeItem(Player player, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack item = player.getInventory().getItemInHand();

        int result = item.getAmount() - amount;
        item.setAmount(result);

        player.setItemInHand(result > 0 ? item : null);
    }

    public static ItemStack getSpawnerItem(EntityType entityType, int amount) {
        ItemStack item = new ItemStack((UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SPAWNER : Material.valueOf("MOB_SPAWNER")), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Methods.compileSpawnerName(entityType, amount));
        CreatureSpawner cs = (CreatureSpawner) ((BlockStateMeta) meta).getBlockState();
        cs.setSpawnedType(entityType);
        ((BlockStateMeta) meta).setBlockState(cs);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getGlass() {
        UltimateStacker instance = UltimateStacker.getInstance();
        return Methods.getGlass(instance.getConfig().getBoolean("Interfaces.Replace Glass Type 1 With Rainbow Glass"), instance.getConfig().getInt("Interfaces.Glass Type 1"));
    }

    public static ItemStack getBackgroundGlass(boolean type) {
        UltimateStacker instance = UltimateStacker.getInstance();
        if (type)
            return getGlass(false, instance.getConfig().getInt("Interfaces.Glass Type 2"));
        else
            return getGlass(false, instance.getConfig().getInt("Interfaces.Glass Type 3"));
    }

    private static ItemStack getGlass(Boolean rainbow, int type) {
        int randomNum = 1 + (int) (Math.random() * 6);
        ItemStack glass;
        if (rainbow) {
            glass = new ItemStack(UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ?
                    Material.LEGACY_STAINED_GLASS_PANE : Material.valueOf("STAINED_GLASS_PANE"), 1, (short) randomNum);
        } else {
            glass = new ItemStack(UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ?
                    Material.LEGACY_STAINED_GLASS_PANE : Material.valueOf("STAINED_GLASS_PANE"), 1, (short) type);
        }
        ItemMeta glassmeta = glass.getItemMeta();
        glassmeta.setDisplayName("§l");
        glass.setItemMeta(glassmeta);
        return glass;
    }

    public static String formatTitle(String text) {
        if (text == null || text.equals(""))
            return "";
        if (!UltimateStacker.getInstance().isServerVersionAtLeast(ServerVersion.V1_9)) {
            if (text.length() > 31)
                text = text.substring(0, 29) + "...";
        }
        text = formatText(text);
        return text;
    }

    public static boolean isInt(String number) {
        if (number == null || number.equals(""))
            return false;
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


    /**
     * Serializes the location of the block specified.
     *
     * @param b The block whose location is to be saved.
     * @return The serialized data.
     */
    public static String serializeLocation(Block b) {
        if (b == null)
            return "";
        return serializeLocation(b.getLocation());
    }

    /**
     * Serializes the location specified.
     *
     * @param location The location that is to be saved.
     * @return The serialized data.
     */
    public static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null)
            return "";
        String w = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        String str = w + ":" + x + ":" + y + ":" + z;
        str = str.replace(".0", "").replace(".", "/");
        return str;
    }

    private static Map<String, Location> serializeCache = new HashMap<>();

    /**
     * Deserializes a location from the string.
     *
     * @param str The string to parse.
     * @return The location that was serialized in the string.
     */
    public static Location unserializeLocation(String str) {
        if (str == null || str.equals(""))
            return null;
        if (serializeCache.containsKey(str)) {
            return serializeCache.get(str).clone();
        }
        String cacheKey = str;
        str = str.replace("y:", ":").replace("z:", ":").replace("w:", "").replace("x:", ":").replace("/", ".");
        List<String> args = Arrays.asList(str.split("\\s*:\\s*"));

        World world = Bukkit.getWorld(args.get(0));
        double x = Double.parseDouble(args.get(1)), y = Double.parseDouble(args.get(2)), z = Double.parseDouble(args.get(3));
        Location location = new Location(world, x, y, z, 0, 0);
        serializeCache.put(cacheKey, location.clone());
        return location;
    }


    public static String convertToInvisibleString(String s) {
        if (s == null || s.equals(""))
            return "";
        StringBuilder hidden = new StringBuilder();
        for (char c : s.toCharArray()) hidden.append(ChatColor.COLOR_CHAR + "").append(c);
        return hidden.toString();
    }

    public static String insertSemicolon(String s) {
        if (s == null || s.equals(""))
            return "";
        StringBuilder hidden = new StringBuilder();
        for (char c : s.toCharArray()) hidden.append(";").append(c);
        return hidden.toString();
    }


    public static String formatText(String text) {
        if (text == null || text.equals(""))
            return "";
        return formatText(text, false);
    }

    public static String formatText(String text, boolean cap) {
        if (text == null || text.equals(""))
            return "";
        if (cap)
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static class Tuple<key, value> {
        public final key x;
        public final value y;
        public Tuple(key x, value y) {
            this.x = x;
            this.y = y;
        }

        public key getKey() {
            return this.x;
        }

        public value getValue() {
            return this.y;
        }
    }
}
