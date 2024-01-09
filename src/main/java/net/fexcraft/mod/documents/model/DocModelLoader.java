package net.fexcraft.mod.documents.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.fexcraft.mod.documents.Documents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.client.model.IModelLoader;

public class DocModelLoader implements IModelLoader<DocModel> {

    public static final DocModelLoader INSTANCE = new DocModelLoader();
    private IResourceManager resman = Minecraft.getInstance().getResourceManager();

    @Override
    public void onResourceManagerReload(IResourceManager iResourceManager){
        resman = iResourceManager;
    }

    @Override
    public DocModel read(JsonDeserializationContext context, JsonObject json){
        if(!json.has("model")){
            Documents.LOGGER.info("NM: " + json);
        }
        return new DocModel(context, json);
    }

}
