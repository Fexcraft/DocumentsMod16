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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class DocEditorContainer extends Container implements UiPacketReceiver {

    protected ItemStack stack;
    protected Document doc;
    protected DocEditorScreen screen;
    protected PlayerEntity player;

    public DocEditorContainer(int id, PlayerInventory inv){
        super(Documents.DOC_EDITOR.get(), id);
        stack = inv.player.getItemInHand(Hand.MAIN_HAND);
        player = inv.player;
        doc = DocRegistry.get(stack);
    }

    public DocEditorContainer(int id, PlayerInventory inv, PacketBuffer buffer){
        this(id, inv);
    }

    @Override
    public boolean stillValid(PlayerEntity player){
        return true;
    }

    @Override
    public void onPacket(CompoundNBT com, boolean client){
        if(com.contains("issue") && com.getBoolean("issue")){
            if(!client && !DocPerms.hasPerm(player, "document.issue", doc.id)){
                player.sendMessage(new TranslationTextComponent("documents.editor.noperm"), player.getGameProfile().getId());
                return;
            }
            issueBy(stack.getTag(), player, client);
            if(!client){
                send(true, com, player);
            }
            else{
                player.closeContainer();
                player.sendMessage(new TranslationTextComponent("documents.editor.signed"), player.getGameProfile().getId());
            }
            return;
        }
        if(!com.contains("field")) return;
        if(!client && !DocPerms.hasPerm(player, "document.edit", doc.id)){
            player.sendMessage(new TranslationTextComponent("documents.editor.noperm"), player.getGameProfile().getId());
            return;
        }
        String field = com.getString("field");
        FieldData data = doc.fields.get(field);
        String value = com.getString("value");
        if(!data.type.editable) return;
        if(!client){
            if(data.type.number()){
                try{
                    if(data.type == FieldType.INTEGER){
                        Integer.parseInt(value);
                    }
                    else{
                        Float.parseFloat(value);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    player.sendMessage(new StringTextComponent("Error: " + e.getMessage()), player.getGameProfile().getId());
                    return;
                }
            }
            else if(data.type == FieldType.DATE){
                try{
                    value = (LocalDate.parse(value).toEpochDay() * 86400000) + "";
                }
                catch(Exception e){
                    e.printStackTrace();
                    player.sendMessage(new StringTextComponent("Error: " + e.getMessage()), player.getGameProfile().getId());
                    return;
                }
            }
            else if(data.type == FieldType.UUID){
                try{
                    GameProfile gp = ServerLifecycleHooks.getCurrentServer().getProfileCache().get(value);
                    if(gp != null && gp.getId() != null && gp.getName() != null){
                        value = gp.getId().toString();
                    }
                    else UUID.fromString(value);
                }
                catch(Exception e){
                    e.printStackTrace();
                    player.sendMessage(new StringTextComponent("Error: " + e.getMessage()), player.getGameProfile().getId());
                    return;
                }
            }
            stack.getTag().putString("document:" + field, value);
            com.putString("value", value);
            send(true, com, player);
        }
        else{
            stack.getTag().putString("document:" + field, value);
            screen.statustext = null;
        }
    }

    private void issueBy(CompoundNBT com, PlayerEntity player, boolean client){
        com.putString("document:issuer", player.getGameProfile().getId().toString());
        com.putString("document:issued", new Date().getTime() + "");
        com.putString("document:issuer_name", player.getGameProfile().getName());
        if(client) return;
        try{
            GameProfile gp = ServerLifecycleHooks.getCurrentServer().getProfileCache().get(UUID.fromString(com.getString("document:uuid")));
            com.putString("document:player_name", gp.getName());
        }
        catch(Exception e){
            e.printStackTrace();
            com.putString("document:player_name", com.getString("document:uuid"));
        }
    }
    
}
