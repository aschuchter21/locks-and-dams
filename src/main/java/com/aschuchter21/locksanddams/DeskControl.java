package com.aschuchter21.locksanddams;

import net.minecraft.nbt.CompoundTag;

/** Controller-owned commands and timers. All durations count loaded simulation ticks. */
public final class DeskControl {
    public static final int CLOSE=0,OPEN=1,HOLD=2,FILL=1,DRAIN=-1;
    public final int[] gate={HOLD,HOLD};
    public final int[] openTicks={0,0};
    public final boolean[] hornArmed={false,false};
    public final boolean[] gateSelection={false,false};
    public int waterSelection=FILL,waterRequest,warningTicks,hornTicks;
    private long warningUntil,hornUntil;
    private final long[] openedAt={-1,-1};
    public boolean stopped;
    public void selectGate(int side,int command) {
        if(stopped)return;gate[side]=command;gateSelection[side]=command==OPEN;
        if(command!=OPEN){hornArmed[side]=false;openTicks[side]=0;hornTicks=0;hornUntil=0;openedAt[side]=-1;}
    }
    public void selectWater(int selection,long now) {
        if(stopped)return;waterSelection=selection;waterRequest=selection;warningTicks=200;warningUntil=now+200;
    }
    public void stop() {stopped=true;idle();}
    public void reset() {idle();stopped=false;}
    public void idle() {
        gate[0]=gate[1]=HOLD;waterRequest=0;warningTicks=0;hornTicks=0;warningUntil=hornUntil=0;
        for(int i=0;i<2;i++){openTicks[i]=0;hornArmed[i]=false;openedAt[i]=-1;}
    }
    public void tick(boolean healthy,boolean lowerOpen,boolean upperOpen,long now) {
        if(!healthy){idle();return;}
        if(stopped)return;
        warningTicks=(int)Math.max(0,warningUntil-now);hornTicks=(int)Math.max(0,hornUntil-now);
        for(int i=0;i<2;i++) {
            boolean open=i==0?lowerOpen:upperOpen;
            if(!open){if(openedAt[i]>=0){hornTicks=0;hornUntil=0;}openedAt[i]=-1;openTicks[i]=0;if(gate[i]==OPEN)hornArmed[i]=true;}
            else if(gate[i]==OPEN&&hornArmed[i]) {
                if(openedAt[i]<0)openedAt[i]=now;openTicks[i]=(int)(now-openedAt[i]);
                if(openTicks[i]>=80){hornTicks=40;hornUntil=now+40;hornArmed[i]=false;}
            }
        }
    }
    public int waterCommand(){return stopped||warningTicks>0?0:waterRequest;}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag();n.putInt("Lower",gate[0]);n.putInt("Upper",gate[1]);n.putInt("WaterSelection",waterSelection);
        n.putBoolean("LowerSelection",gateSelection[0]);n.putBoolean("UpperSelection",gateSelection[1]);n.putBoolean("Stopped",stopped);return n;
    }
    public void load(CompoundTag n) {
        // Loading/relinking never starts machinery from an old switch position.
        idle();waterSelection=n.getInt("WaterSelection")==DRAIN?DRAIN:FILL;gateSelection[0]=n.getBoolean("LowerSelection");gateSelection[1]=n.getBoolean("UpperSelection");stopped=n.getBoolean("Stopped");
    }
}
