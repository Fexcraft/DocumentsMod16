package net.fexcraft.mod.documents;

import net.fexcraft.mod.documents.gui.DocEditorScreen;
import net.fexcraft.mod.documents.gui.DocViewerScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "documents", bus = Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event){
        ScreenManager.register(Documents.DOC_EDITOR.get(), DocEditorScreen::new);
        ScreenManager.register(Documents.DOC_VIEWER.get(), DocViewerScreen::new);
    }

}
