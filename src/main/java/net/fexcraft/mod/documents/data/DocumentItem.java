package net.fexcraft.mod.documents.data;

import java.util.List;

import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.gui.DocEditorContainer;
import net.fexcraft.mod.documents.DocRegistry;
import net.fexcraft.mod.documents.gui.DocViewerContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ObjectHolder;

public class DocumentItem extends Item {

	@ObjectHolder("documents:document")
	public static final DocumentItem INSTANCE = null;
	public static String NBTKEY = "documents:type";

	public DocumentItem(){
		super(new Properties().fireResistant().tab(ItemGroup.TAB_MISC).stacksTo(1));
		setRegistryName("documents", "document");
	}

	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> list, ITooltipFlag flag){
		if(stack.hasTag() && stack.getTag().contains(NBTKEY)){
			CompoundNBT com = stack.getTag();
			Document doc = DocRegistry.DOCS.get(com.getString(NBTKEY));
			if(doc == null){
				list.add(new StringTextComponent("no document data"));
				list.add(new StringTextComponent(com.toString()));
			}
			else{
				for(String str : doc.description){
					list.add(new TranslationTextComponent(str));
				}
				list.add(new TranslationTextComponent(com.getBoolean("document:issued") ? "documents.item.issued" : "documents.item.blank"));
			}
		}
		else{
			list.add(new StringTextComponent("no type data"));
		}
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand){
		if(world.isClientSide) return ActionResult.pass(player.getItemInHand(hand));
		ItemStack stack = player.getItemInHand(hand);
		if(stack.hasTag() && stack.getTag().contains(NBTKEY)){
			CompoundNBT com = stack.getTag();
			Document doc = DocRegistry.DOCS.get(com.getString(NBTKEY));
			if(doc == null){
				player.sendMessage(new StringTextComponent("no document data"), null);
				return ActionResult.fail(stack);
			}
			else{
				NetworkHooks.openGui((ServerPlayerEntity)player, new INamedContainerProvider() {
					@Override
					public ITextComponent getDisplayName(){
						return new StringTextComponent(com.contains("document:issued") ? "Document Viewer" : "Document Editor");
					}

					@Override
					public Container createMenu(int i, PlayerInventory pinv, PlayerEntity ent){
						return com.contains("document:issued") ? new DocViewerContainer(i, pinv) : new DocEditorContainer(i, pinv);
					}
				}, buf -> buf.writeInt(0));
				return ActionResult.success(stack);
			}
		}
		else return ActionResult.pass(stack);
		//return ActionResult.success(player.getItemInHand(hand));
	}

}
