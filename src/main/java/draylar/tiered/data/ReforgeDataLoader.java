package draylar.tiered.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class ReforgeDataLoader extends SimplePreparableReloadListener implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TieredZ");

    private final List<Identifier> reforgeIdentifiers = new ArrayList<>();
    private final Map<Identifier, List<Item>> reforgeBaseMap = new HashMap<>();

    @Override
    public @NonNull Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("tiered", "reforge_loader");
    }

    @Override
    public Object prepare(ResourceManager resourceManager, @NonNull ProfilerFiller profilerFiller) {

        resourceManager.listResources("reforge_items", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try {
                InputStream stream = resourceRef.open();
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                for (int u = 0; u < data.getAsJsonArray("items").size(); u++) {
                    if (BuiltInRegistries.ITEM.get(Identifier.parse(data.getAsJsonArray("items").get(u).getAsString())).toString().equals("air")) {
                        LOGGER.info("Resource {} was not loaded cause {} is not a valid item identifier", id.toString(), data.getAsJsonArray("items").get(u).getAsString());
                        continue;
                    }
                    List<Item> baseItems = new ArrayList<Item>();
                    for (int i = 0; i < data.getAsJsonArray("base").size(); i++) {
                        if (BuiltInRegistries.ITEM.get(Identifier.parse(data.getAsJsonArray("base").get(i).getAsString())).toString().equals("air")) {
                            LOGGER.info("Resource {} was not loaded cause {} is not a valid item identifier", id.toString(), data.getAsJsonArray("base").get(i).getAsString());
                            continue;
                        }
                        baseItems.add(BuiltInRegistries.ITEM.get(Identifier.parse(data.getAsJsonArray("base").get(i).getAsString())).get().value());
                    }

                    reforgeIdentifiers.add(Identifier.parse(data.getAsJsonArray("items").get(u).getAsString()));
                    reforgeBaseMap.put(Identifier.parse(data.getAsJsonArray("items").get(u).getAsString()), baseItems);
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while loading resource {}. {}", id.toString(), e.toString());
            }
        });
        return null;
    }

    public List<Item> getReforgeBaseItems(Item item) {
        ArrayList<Item> list = new ArrayList<Item>();
        if (reforgeBaseMap.containsKey(BuiltInRegistries.ITEM.getId(item))) {
            return reforgeBaseMap.get(BuiltInRegistries.ITEM.getId(item));
        }
        return list;
    }

    public void putReforgeBaseItems(Identifier id, List<Item> items) {
        reforgeBaseMap.put(id, items);
    }

    public void clearReforgeBaseItems() {
        reforgeBaseMap.clear();
    }

    public List<Identifier> getReforgeIdentifiers() {
        return reforgeIdentifiers;
    }


    @Override
    protected void apply(Object object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {

    }
}
