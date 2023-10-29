package net.fexcraft.mod.documents.gui;

import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.packet.GuiPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.PacketDistributor;

public interface UiPacketReceiver {

	public void onPacket(CompoundNBT com, boolean client);

	public default void send(boolean toclient, CompoundNBT compound, PlayerEntity player){
		if(toclient) Documents.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), new GuiPacket(compound));
		else Documents.CHANNEL.sendToServer(new GuiPacket(compound));
	}

}
