package jeresources.entry;

import jeresources.api.distributions.DistributionBase;
import jeresources.api.distributions.DistributionHelpers;
import jeresources.api.drop.LootDrop;
import jeresources.api.render.ColourHelper;
import jeresources.api.restrictions.Restriction;
import jeresources.util.MapKeys;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

public class WorldGenEntry {
    private float[] chances;
    private boolean silktouch;
    private ItemStack block;
    private int minY;
    private int maxY;
    private int colour;
    private Restriction restriction;
    private DistributionBase distribution;
    private Map<String, Set<LootDrop>> drops;
    private Map<Item, Set<LootDrop>> wildcardDrops;
    private Map<String, ItemStack> dropsDisplay;

    public WorldGenEntry(ItemStack block, DistributionBase distribution, Restriction restriction, boolean silktouch, LootDrop... drops) {
        this.block = block;
        this.distribution = distribution;
        this.restriction = restriction;
        this.colour = ColourHelper.BLACK;
        this.silktouch = silktouch;
        this.drops = new HashMap<>();
        this.wildcardDrops = new HashMap<>();
        this.dropsDisplay = new HashMap<>();
        addDrops(drops);
        calcChances();
    }

    public WorldGenEntry(ItemStack block, DistributionBase distribution, LootDrop... drops) {
        this(block, distribution, Restriction.OVERWORLD, false, drops);
    }

    public WorldGenEntry(ItemStack block, DistributionBase distribution, boolean silktouch, LootDrop... drops) {
        this(block, distribution, Restriction.OVERWORLD, silktouch, drops);
    }

    public WorldGenEntry(ItemStack block, DistributionBase distribution, Restriction restriction, LootDrop... drops) {
        this(block, distribution, restriction, false, drops);
    }

    public void addDrops(LootDrop... drops) {
        for (LootDrop drop : drops) {
            String mapKey = MapKeys.getKey(drop.item);
            if (mapKey == null) continue;
            Set<LootDrop> dropSet = this.drops.get(mapKey);
            if (dropSet == null) dropSet = new TreeSet<>();
            dropSet.add(drop);
            this.drops.put(mapKey, dropSet);
            if (drop.item.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                Set<LootDrop> wildcardDropSet = this.wildcardDrops.get(drop.item.getItem());
                if (wildcardDropSet == null) wildcardDropSet = new TreeSet<>();
                wildcardDropSet.add(drop);
                this.wildcardDrops.put(drop.item.getItem(), wildcardDropSet);
            }
            if (!this.dropsDisplay.containsKey(mapKey)) {
                ItemStack itemStack = drop.item.copy();
                itemStack.stackSize = Math.max(1, drop.minDrop);
                this.dropsDisplay.put(mapKey, itemStack);
            }
        }
    }

    public void addDrops(Collection<LootDrop> drops) {
        addDrops(drops.toArray(new LootDrop[drops.size()]));
    }

    private void calcChances() {
        chances = new float[256];
        minY = 256;
        maxY = 0;
        int i = -1;
        for (float chance : this.distribution.getDistribution()) {
            if (++i == chances.length) break;
            chances[i] += chance;
            if (chances[i] > 0) {
                if (minY > i)
                    minY = i;
                if (i > maxY)
                    maxY = i;
            }
        }
        if (minY == 256) minY = 0;
        if (maxY == 0) maxY = 255;

        if (minY < 128)
            minY = 0;
        else
            minY = 128;

        if (maxY <= 127)
            maxY = 127;
        else
            maxY = 255;
    }

    public float[] getChances() {
        return Arrays.copyOfRange(chances, minY, maxY + 1);
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public boolean isSilkTouchNeeded() {
        return silktouch;
    }

    public int getColour() {
        return colour;
    }

    public List<ItemStack> getDrops() {
        return new ArrayList<>(this.dropsDisplay.values());
    }

    public List<ItemStack> getBlockAndDrops() {
        List<ItemStack> list = new LinkedList<>();
        list.add(this.block);
        list.addAll(getDrops());
        return list;
    }

    public ItemStack getBlock() {
        return this.block;
    }

    public List<String> getBiomeRestrictions() {
        return this.restriction.getBiomeRestrictions();
    }

    public String getDimension() {
        return this.restriction.getDimensionRestriction();
    }

    public List<LootDrop> getLootDrops(ItemStack itemStack) {
        String key = MapKeys.getKey(itemStack);
        List<LootDrop> list = new ArrayList<>(this.drops.containsKey(key) ? this.drops.get(key) : this.wildcardDrops.get(itemStack.getItem()));
        Collections.reverse(list);
        return list;
    }

    @Override
    public String toString() {
        return "WorldGenEntry: " + block.getDisplayName() + " - " + restriction.toString();
    }

    public Restriction getRestriction() {
        return restriction;
    }

    public void merge(WorldGenEntry entry) {
        entry.drops.values().forEach(this::addDrops);
        this.distribution = DistributionHelpers.addDistribution(this.distribution, entry.distribution);
        calcChances();
    }
}
