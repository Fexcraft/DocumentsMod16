package net.fexcraft.mod.documents.packet;

import net.fexcraft.app.json.JsonMap;
import net.minecraft.nbt.CompoundNBT;

public class SyncPacket {

    public JsonMap map;

    public SyncPacket(JsonMap map){
        this.map = map;
    }

}
