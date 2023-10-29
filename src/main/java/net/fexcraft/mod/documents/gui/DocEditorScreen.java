package net.fexcraft.mod.documents.gui;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.ExternalTextures;
import net.fexcraft.mod.documents.data.FieldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DocEditorScreen extends ContainerScreen<DocEditorContainer> {

    public static final ResourceLocation TEXTURE = new ResourceLocation("documents:textures/gui/editor.png");
    private GenericButton[] fieldbuttons = new GenericButton[9];
    private GenericButton[] concanbuttons = new GenericButton[3];
    private GenericText[] infotext = new GenericText[4];
    private GenericText valueinfo, status;
    private String[] fieldkeys;
    private int scroll;
    private int selected = -1;
    private FieldData data;
    private int todo;
    protected String statustext;
    protected TextFieldWidget field;
    //
    private static DocEditorScreen SCREEN;
    private static ResourceLocation tempimg;

    public DocEditorScreen(DocEditorContainer container, PlayerInventory inventory, ITextComponent component){
        super(container, inventory, component);
        imageWidth = 256;
        imageHeight = 104;
        if(container.doc == null){
            inventory.player.sendMessage(new StringTextComponent("item.missing.doc"), null);
            inventory.player.closeContainer();
        }
        SCREEN = this;
    }

    @Override
    protected void init(){
        super.init();
        menu.screen = this;
        Object[] entries = menu.doc.fields.entrySet().stream().filter(e -> e.getValue().type.editable).collect(Collectors.toList()).toArray();
        ArrayList<String> list = new ArrayList<>();
        for(Object obj : entries) list.add(((java.util.Map.Entry<String, FieldData>)obj).getKey());
        fieldkeys = list.toArray(new String[0]);
        for(int i = 0; i < fieldbuttons.length; i++){
            int I = i;
            buttons.add(fieldbuttons[i] = new GenericButton(leftPos + 17, topPos + 8 + i * 10, 17, 8 + i * 10, 48, 8, new StringTextComponent("")){
                @Override
                public void onPress(){
                    if(I + scroll >= fieldkeys.length) return;
                    data = menu.doc.fields.get(fieldkeys[selected = I + scroll]);
                    if(data.type.image()){
                        String url = menu.stack.getTag().getString("document:" + data.name);
                        if(url == null || url.length() == 0) url = data.value;
                        if(url != null && url.length() > 0){
                            if(url.startsWith("external;") || url.startsWith("http")){
                                if(url.startsWith("external;")) url = url.substring(9);
                                tempimg = ExternalTextures.get(url);
                            }
                            else tempimg = new ResourceLocation(data.value);
                        }
                        else tempimg = null;
                        Documents.LOGGER.info(url + " " + tempimg);
                        field.setMaxLength(1024);
                    }
                    else field.setMaxLength(128);
                    field.setVisible(false);
                    if(data.type.number() || data.type.editable){
                        String val = data.getValue(menu.stack);
                        field.setValue(val == null ? "" : val);
                        field.setVisible(true);
                        field.setFilter(str -> {
                            if(data.type.number()){

                            }
                            else{

                            }
                            return true;
                        });
                    }
                    statustext = null;
                }
            }.text(true));
            //this.texts.put("f" + i, new BasicText(guiLeft + 18, guiTop + 8 + i * 10, 46, null, "...").autoscale());
        }
        buttons.add(new GenericButton(leftPos + 7, topPos + 7, 7, 7, 7, 7, new StringTextComponent("up")){
            @Override
            public void onPress(){
                if(scroll > 0) scroll--;
            }
        });
        buttons.add(new GenericButton(leftPos + 7, topPos + 90, 7, 90, 7, 7, new StringTextComponent("down")){
            @Override
            public void onPress(){
                if(scroll < fieldkeys.length - 1) scroll++;
            }
        });
        for(int i = 0; i < infotext.length; i++){
            buttons.add(infotext[i] = new GenericText(leftPos + 71, topPos + 10 + i * 12, 125, "").autoscale());
        }
        buttons.add(valueinfo = new GenericText(leftPos + 71, topPos + 60, 175, "...").autoscale().color(0xffffff));
        buttons.add(status = new GenericText(leftPos + 69, topPos + 87, 153, "...").autoscale().color(0x000000));
        children.add(field = new TextFieldWidget(minecraft.font, leftPos + 70, topPos + 71, 166, 10, new TranslationTextComponent("...")));
        field.setVisible(false);
        buttons.add(concanbuttons[0] = new GenericButton(leftPos + 237, topPos + 71, 237, 71, 10, 10, "confirm_value"){
            @Override
            public void onPress(){
                if(data == null || !data.type.editable) return;
                if(data.type.number()){
                    Float val = Float.parseFloat(field.getValue());
                    if(val == null){
                        statustext = "&cinvalid number input";
                    }
                    else{
                        CompoundNBT compound = new CompoundNBT();
                        compound.putString("field", fieldkeys[selected]);
                        compound.putString("value", val + "");
                        menu.send(false, compound, menu.player);
                    }
                }
                else{
                    CompoundNBT compound = new CompoundNBT();
                    compound.putString("field", fieldkeys[selected]);
                    compound.putString("value", field.getValue());
                    menu.send(false, compound, menu.player);
                }
            }
        });
        buttons.add(concanbuttons[1] = new GenericButton(leftPos + 224, topPos + 85, 224, 85, 12, 12, "cancel"){
            @Override
            public void onPress(){
                inventory.player.closeContainer();
                minecraft.setScreen(null);
            }
        });
        buttons.add(concanbuttons[2] = new GenericButton(leftPos + 237, topPos + 85, 237, 85, 12, 12, "confirm"){
            @Override
            public void onPress(){
                if(todo > 0){
                    statustext = "documents.editor.status.incomplete";
                    return;
                }
                CompoundNBT compound = new CompoundNBT();
                compound.putBoolean("issue", true);
                menu.send(false, compound, menu.player);
            }
        });
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers){
        if(pKeyCode == 256){
            this.minecraft.player.closeContainer();
            return true;
        }
        if(field.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        if(field.isFocused()) return false;
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers){
        return field.isFocused() && field.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton){
        if(field.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        for(Widget button : buttons) if(button.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected void renderBg(MatrixStack matrix, float ticks, int x, int y){
        for(int i = 0; i < fieldbuttons.length; i++){
            int I = i + scroll;
            if(I >= fieldkeys.length){
                fieldbuttons[i].visible = false;
                fieldbuttons[i].setMessage(new StringTextComponent(""));
            }
            else{
                fieldbuttons[i].visible = true;
                fieldbuttons[i].setMessage(new StringTextComponent(menu.doc.fields.get(fieldkeys[I]).name));
            }
        }
        boolean ex = selected > -1 && data != null;
        for(int i = 0; i < infotext.length; i++){
            if(ex){
                infotext[i].setMessage(i >= data.description.size() ? new StringTextComponent("") : new TranslationTextComponent(data.description.get(i)));
            }
            else infotext[i].setMessage(new StringTextComponent(""));
        }
        valueinfo.setMessage(new StringTextComponent(ex && data.value != null ? data.value : ""));
        getStatus();
        //
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.textureManager.bind(TEXTURE);
        blit(matrix, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        RenderSystem.disableRescaleNormal();
        RenderSystem.disableDepthTest();
        for(IGuiEventListener w : children) if(w instanceof Widget) ((Widget)w).render(matrix, x, y, ticks);
        //
        RenderSystem.enableBlend();
        RenderSystem.enableAlphaTest();
        if(ex && data.type.image() && tempimg != null){
            minecraft.textureManager.bind(tempimg);
            draw(matrix.last().pose(), leftPos + 199, topPos + 9, 48, 48);
        }
    }
    
    public static void draw(Matrix4f matrix, int x, int y, int w, int h){
        BufferBuilder buffer = Tessellator.getInstance().getBuilder();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.vertex(matrix, x, y + h, 0).uv(0, 1).endVertex();
        buffer.vertex(matrix, x + w, y + h, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, x + w, y, 0).uv(1, 0).endVertex();
        buffer.vertex(matrix, x, y, 0).uv(0, 0).endVertex();
        buffer.end();
        WorldVertexBufferUploader.end(buffer);
    }

    private void getStatus(){
        if(statustext != null){
            status.setMessage(new TranslationTextComponent(statustext));
            return;
        }
        todo = 0;
        String eg = null;
        for(String str : fieldkeys){
            FieldData data = menu.doc.fields.get(str);
            if(!data.type.editable) continue;
            String val = data.getValue(menu.stack);
            if(val == null || val.length() == 0 && !data.can_empty){
                todo++;
                if(eg == null) eg = str;
            }
        }
        if(todo > 0){
            status.setMessage(new TranslationTextComponent("documents.editor.status.todo", todo, eg));
        }
        else{
            status.setMessage(new TranslationTextComponent("documents.editor.status.done"));
        }
    }

    @Override
    protected void renderLabels(MatrixStack stack, int x, int y){
        //
    }

    @Override
    public void tick(){
        super.tick();
        field.tick();
    }

    public static abstract class GenericButton extends AbstractButton {

        private int tx, ty;
        private boolean text;
        private ResourceLocation texture = TEXTURE;

        public GenericButton(int x, int y, int tx, int ty,int w, int h, ITextComponent text){
            super(x, y, w, h, text);
            this.tx = tx;
            this.ty = ty;
        }

        public GenericButton(int x, int y, int tx, int ty,int w, int h, String text){
            this(x, y, tx, ty, w, h, new StringTextComponent(text));
        }

        public GenericButton(int x, int y, int tx, int ty,int w, int h){
            this(x, y, tx, ty, w, h, "");
        }

        public GenericButton text(boolean bool){
            text = bool;
            return this;
        }

        public void renderButton(MatrixStack stack, int mx, int my, float ticks){
            Minecraft.getInstance().getTextureManager().bind(texture);
            if(isHovered) RenderSystem.color4f(0.85f, 0.7f, 0.18f, 0.75f);
            blit(stack, x, y, tx, ty, width, height);
            RenderSystem.color4f(1, 1, 1, 1);
            if(text) drawCenteredString(stack, Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, getFGColor());
        }

    }

    public static class GenericText extends Widget {

        protected Integer color = 0x636363;
        protected boolean centered;
        protected float scale = 1;
        protected String text, temp;

        public GenericText(int x, int y, int w, String text){
            super(x, y, w, 8, new StringTextComponent(text));
            this.text = text;
        }

        public GenericText(int x, int y, int w, ITextComponent com){
            super(x, y, w, 8, com);
            this.text = com.getString();
        }

        public GenericText autoscale(){
            scale = -1;
            return this;
        }

        @Override
        public void renderButton(MatrixStack stack, int mx, int my, float ticks){
            temp = getMessage().getString();
            if(temp == null) return;
            if(!centered){
                stack.pushPose();
                stack.translate(x, y, 0);
                if(scale != 1){
                    if(scale == -1){
                        float w = (float)Minecraft.getInstance().font.width(temp);
                        if(w > 0){
                            float s = width / w;
                            if(s > 1) s = 1;
                            stack.scale(s, s, s);
                        }
                    }
                    else{
                        stack.scale(scale, scale, scale);
                    }
                }
                Minecraft.getInstance().font.drawInternal(temp, 0, 0, color == null ? getFGColor() : color, stack.last().pose(), false, false);
                stack.popPose();
            }
            //if(!centered) drawString(stack, Minecraft.getInstance().font, getMessage(), x, y, color == null ? getFGColor() : color);
            //else drawCenteredString(stack, Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, color == null ? getFGColor() : color);
            else{
                FontRenderer font = Minecraft.getInstance().font;
                Minecraft.getInstance().font.drawInternal(temp, x + width / 2 - (font.width(temp) / 2), y + (height - 8) / 2, color == null ? getFGColor() : color, stack.last().pose(), false, false);
            }
            if(this.isHovered) renderToolTip(stack, mx, my);
        }

        @Override
        public void renderToolTip(MatrixStack stack, int mx, int my){
            if(getMessage() == null || getMessage().getString() == null) return;
            try{
                SCREEN.renderTooltip(stack, getMessage(), mx, my);
            }
            catch(Exception e){
                //
            }
        }

        public GenericText color(Integer col){
            color = col;
            return this;
        }

        public GenericText centered(boolean bool){
            centered = bool;
            return this;
        }

        @Override
        public void setMessage(ITextComponent pMessage){
            super.setMessage(pMessage);
            this.text = pMessage.getString();
        }

        public GenericText scale(float scale){
            this.scale = scale;
            return this;
        }
    }

}
