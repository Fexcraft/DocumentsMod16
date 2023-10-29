package net.fexcraft.mod.documents.packet;

import net.fexcraft.app.json.JsonMap;
import net.minecraft.nbt.CompoundNBT;

public class GuiPacket {

    public CompoundNBT com;

    public GuiPacket(CompoundNBT com){
        this.com = com;
    }

}
