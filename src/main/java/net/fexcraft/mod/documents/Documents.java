package net.fexcraft.mod.documents;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fexcraft.app.json.JsonHandler;
import net.fexcraft.app.json.JsonHandler.PrintOption;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.mod.documents.data.Document;
import net.fexcraft.mod.documents.data.DocumentItem;
import net.fexcraft.mod.documents.gui.DocEditorContainer;
import net.fexcraft.mod.documents.gui.DocViewerContainer;
import net.fexcraft.mod.documents.gui.UiPacketReceiver;
import net.fexcraft.mod.documents.packet.GuiPacket;
import net.fexcraft.mod.documents.packet.SyncPacket;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("documents")
public class Documents {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, "documents");
    public static final RegistryObject<ContainerType<DocEditorContainer>> DOC_EDITOR = CONTAINERS.register("editor", () -> IForgeContainerType.create(DocEditorContainer::new));
    public static final RegistryObject<ContainerType<DocViewerContainer>> DOC_VIEWER = CONTAINERS.register("viewer", () -> IForgeContainerType.create(DocViewerContainer::new));
    public static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("documents", "channel"))
            .clientAcceptedVersions(pro -> true)
            .serverAcceptedVersions(pro -> true)
            .networkProtocolVersion(() -> "protocol")
            .simpleChannel();

    public Documents(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        CONTAINERS.register(bus);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event){
        DocRegistry.init();
        DocPerms.loadperms();
        CHANNEL.registerMessage(0, SyncPacket.class,
            (packet, buffer) -> {
                String str = JsonHandler.toString(packet.map, PrintOption.FLAT);
                buffer.writeUtf(str);
            },
            (buffer) -> {
                JsonMap map = JsonHandler.parse(buffer.readUtf(), true).asMap();
                return new SyncPacket(map);
            },
            (packet, context) -> {
                context.get().enqueueWork(() -> {
                    LOGGER.info(packet.map);
                    DocRegistry.load(packet.map);
                    DocRegistry.DOCS.values().forEach(doc -> doc.linktextures());
                });
                context.get().setPacketHandled(true);
            }
        );
        CHANNEL.registerMessage(1, GuiPacket.class,
                (packet, buffer) -> {
                    buffer.writeNbt(packet.com);
                },
                (buffer) -> {
                    return new GuiPacket(buffer.readNbt());
                },
                (packet, context) -> {
                    context.get().enqueueWork(() -> {
                        if(context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT){
                            ((UiPacketReceiver)net.minecraft.client.Minecraft.getInstance().player.containerMenu).onPacket(packet.com, true);
                        }
                        else{
                            ((UiPacketReceiver)context.get().getSender().containerMenu).onPacket(packet.com, false);
                        }
                    });
                    context.get().setPacketHandled(true);
                }
        );
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event){
        //
    }

    @Mod.EventBusSubscriber(modid = "documents", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Item> event){
            event.getRegistry().register(new DocumentItem());
        }

    }

    @Mod.EventBusSubscriber(modid = "documents")
    public static class Events {

        @SubscribeEvent
        public static void onPlayerJoin(PlayerLoggedInEvent event){
            if(event.getPlayer().level.isClientSide) return;
            DocRegistry.opj(event.getPlayer());
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)event.getPlayer()), new SyncPacket(DocRegistry.confmap));
            //LOGGER.info(event.getPlayer().getName().getString() + " IN");
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerLoggedOutEvent event){
            if(event.getPlayer().level.isClientSide) return;
            DocRegistry.opl(event.getPlayer());
            //LOGGER.info(event.getPlayer().getName().getString() + " OUT");
        }

        @SubscribeEvent
        public static void onCmdReg(RegisterCommandsEvent event){
            event.getDispatcher().register(Commands.literal("documents")
                .then(Commands.literal("list").executes(cmd -> {
                    cmd.getSource().sendSuccess(new StringTextComponent("\u00A77============"), true);
                    for(String str : DocRegistry.DOCS.keySet()){
                        cmd.getSource().sendSuccess(new StringTextComponent(str), true);
                    }
                    return 0;
                }))
                .then(Commands.literal("uuid").executes(cmd -> {
                    LOGGER.info(cmd.getSource().getPlayerOrException());
                    cmd.getSource().sendSuccess(new StringTextComponent(cmd.getSource().getPlayerOrException().getGameProfile().getId().toString()), true);
                    return 0;
                }))
                .then(Commands.literal("reload-perms").executes(cmd -> {
                    PlayerEntity entity = cmd.getSource().getPlayerOrException();
                    if(!DocPerms.hasPerm(entity, "command.reload-perms") && !entity.hasPermissions(4)){
                        cmd.getSource().sendFailure(new TranslationTextComponent("documents.cmd.no_permission"));
                        return 1;
                    }
                    DocPerms.loadperms();
                    cmd.getSource().sendSuccess(new TranslationTextComponent("documents.cmd.perms_reloaded"), true);
                    return 0;
                }))
                .then(Commands.literal("reload-docs").executes(cmd -> {
                    PlayerEntity entity = cmd.getSource().getPlayerOrException();
                    if(!DocPerms.hasPerm(entity, "command.reload-docs") && !entity.hasPermissions(4)){
                        cmd.getSource().sendFailure(new TranslationTextComponent("documents.cmd.no_permission"));
                        return 1;
                    }
                    DocRegistry.init();
                    entity.getServer().getPlayerList().getPlayers().forEach(player -> {
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPacket(DocRegistry.confmap));
                    });
                    cmd.getSource().sendSuccess(new TranslationTextComponent("documents.cmd.docs_reloaded"), true);
                    return 0;
                }))
                .then(Commands.literal("get").then(Commands.argument("id", StringArgumentType.word()).executes(cmd -> {
                    Document doc = DocRegistry.DOCS.get(cmd.getArgument("id", String.class));
                    if(doc == null){
                        cmd.getSource().sendFailure(new TranslationTextComponent("documents.cmd.doc_not_found"));
                        return -1;
                    }
                    else{
                        if(!DocPerms.hasPerm(cmd.getSource().getPlayerOrException(), "command.get", doc.id)){
                            cmd.getSource().sendFailure(new TranslationTextComponent("documents.cmd.no_permission"));
                            return -1;
                        }
                        ItemStack stack = new ItemStack(DocumentItem.INSTANCE);
                        CompoundNBT com = stack.hasTag() ? stack.getTag() : new CompoundNBT();
                        com.putString(DocumentItem.NBTKEY, doc.id);
                        stack.setTag(com);
                        cmd.getSource().getPlayerOrException().addItem(stack);
                        cmd.getSource().sendSuccess(new TranslationTextComponent("documents.cmd.added"), true);
                        LOGGER.info(com);
                    }
                    return 0;
                })))
                .executes(cmd -> {
                    cmd.getSource().sendSuccess(new StringTextComponent("\u00A77============"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("/documents list"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("/documents get"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("/documents uuid"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("/documents reload-perms"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("/documents reload-docs"), true);
                    cmd.getSource().sendSuccess(new StringTextComponent("\u00A77============"), true);
                    return 0;
                })
            );
        }

    }

}
