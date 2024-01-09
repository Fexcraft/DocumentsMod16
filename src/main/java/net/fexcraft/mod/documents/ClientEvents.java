package net.fexcraft.mod.documents;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.fexcraft.mod.documents.gui.DocEditorScreen;
import net.fexcraft.mod.documents.gui.DocViewerScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.model.*;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

@EventBusSubscriber(modid = "documents", bus = Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event){
        ScreenManager.register(Documents.DOC_EDITOR.get(), DocEditorScreen::new);
        ScreenManager.register(Documents.DOC_VIEWER.get(), DocViewerScreen::new);
    }

}
