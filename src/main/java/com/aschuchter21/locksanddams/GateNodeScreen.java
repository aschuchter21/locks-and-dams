package com.aschuchter21.locksanddams;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GateNodeScreen extends AbstractContainerScreen<GateNodeMenu>{
    public GateNodeScreen(GateNodeMenu m,Inventory i,Component t){super(m,i,t);imageWidth=280;imageHeight=166;}
    @Override protected void init(){super.init();
        addRenderableWidget(Button.builder(Component.literal("Lower gate"),b->choose(0)).bounds(leftPos+18,topPos+47,116,22).build());
        addRenderableWidget(Button.builder(Component.literal("Upper gate"),b->choose(1)).bounds(leftPos+146,topPos+47,116,22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(leftPos+90,topPos+130,100,22).build());
    }
    private void choose(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff18252c);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xff34444b);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,"GATE CONTROL NODE",18,13,0xffead7a1,false);
        g.drawString(font,"Selected: "+(menu.upper()?"UPPER GATE":"LOWER GATE"),18,31,0xffa8e4b3,false);
        g.drawString(font,"Front: CLUTCH  |  Powered = stop",18,83,0xffeeeeee,false);
        g.drawString(font,"Top: REVERSE  |  Powered = close",18,98,0xffeeeeee,false);
        g.drawString(font,"Changes apply immediately.",18,115,0xffc3cdd1,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
