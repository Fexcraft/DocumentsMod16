package net.fexcraft.mod.documents.gui;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.fexcraft.mod.documents.DocPerms;
import net.fexcraft.mod.documents.DocRegistry;
import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.data.Document;
import net.fexcraft.mod.documents.data.FieldData;
import net.fexcraft.mod.documents.data.FieldType;
import net.fexcraft.mod.documents.packet.GuiPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class DocViewerContainer extends Container implements UiPacketReceiver {

    protected ItemStack stack;
    protected Document doc;
    protected DocViewerScreen screen;
    protected PlayerEntity player;
    protected int page = 0;

    public DocViewerContainer(int id, PlayerInventory inv){
        super(Documents.DOC_VIEWER.get(), id);
        stack = inv.player.getItemInHand(Hand.MAIN_HAND);
        player = inv.player;
        doc = DocRegistry.get(stack);
    }

    public DocViewerContainer(int id, PlayerInventory inv, PacketBuffer buffer){
        this(id, inv);
        page = buffer.readInt();
    }

    @Override
    public boolean stillValid(PlayerEntity player){
        return true;
    }

    @Override
    public void onPacket(CompoundNBT com, boolean client){
        Documents.LOGGER.info(client + " " + com);
        if(com.contains("open_page")){
            if(client) return;
            NetworkHooks.openGui((ServerPlayerEntity)player, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName(){
                    return new StringTextComponent("Document Viewer");
                }

                @Override
                public Container createMenu(int i, PlayerInventory pinv, PlayerEntity ent){
                    return new DocViewerContainer(i, pinv);
                }
            }, buf -> buf.writeInt(com.getInt("open_page")));
            return;
        }
    }

    public String getValue(String str){
        return stack.getTag().getString("document:" + str);
    }

}
