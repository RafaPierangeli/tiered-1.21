package draylar.tiered.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.config.ConfigInit; // 🌟 Não esqueça de importar a sua config!
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AttributeDataLoader extends SimplePreparableReloadListener implements IdentifiableResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeDataLoader.class);
    private static final String DATA_TYPE = "item_attributes";

    private Map<Identifier, PotentialAttribute> itemAttributes = new HashMap<>();

    @Override
    public @NonNull Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("tiered", "item_attributes");
    }

    @Override
    public Object prepare(ResourceManager resourceManager, @NonNull ProfilerFiller profilerFiller) {
        Map<Identifier, PotentialAttribute> readItemAttributes = new HashMap<>();

        // Encontra todos os arquivos .json dentro de data/<namespace>/item_attributes/
        Map<Identifier, Resource> resources = resourceManager.listResources(DATA_TYPE, id -> id.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();

            // 🌟 1. O FILTRO DA CONFIGURAÇÃO
            // Se a config estiver desligada E o arquivo for do nosso mod ("tiered"), pula ele!
            if (!ConfigInit.CONFIG.enableDefaultModifiers && fileId.getNamespace().equals("tiered")) {
                continue;
            }

            String path = fileId.getPath();
            String attributeName = path.substring(DATA_TYPE.length() + 1, path.length() - 5);
            Identifier attributeId = Identifier.fromNamespaceAndPath(fileId.getNamespace(), attributeName);

            try (Reader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);

                PotentialAttribute attribute = PotentialAttribute.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                        .getOrThrow(error -> new IllegalArgumentException("Falha ao fazer parse do atributo: " + error));

                attribute.setID(attributeId.toString());
                readItemAttributes.put(attributeId, attribute);

            } catch (Exception e) {
                // 🌟 2. A BLINDAGEM CONTRA OUTROS MODS
                if (fileId.getNamespace().equals("tiered")) {
                    // Se for um erro num arquivo NOSSO, avisa no console (pois nós ou o jogador digitamos algo errado no JSON)
                    LOGGER.error("Erro de parsing ao carregar o tier {}", attributeId, e);
                } else {
                    // Se for de outro mod, ignora silenciosamente!
                    // (Usamos .debug para que só apareça se o desenvolvedor ligar o modo de depuração)
                    LOGGER.debug("Ignorando arquivo {} de outro mod (estrutura incompatível com o Tiered).", attributeId);
                }
            }
        }

        this.itemAttributes = readItemAttributes;
        LOGGER.info("Carregados {} tiers de atributos", readItemAttributes.size());
        return null;
    }

    public Map<Identifier, PotentialAttribute> getItemAttributes() {
        return itemAttributes;
    }


    @Override
    public void apply(Object object, @NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profilerFiller) {

    }

}