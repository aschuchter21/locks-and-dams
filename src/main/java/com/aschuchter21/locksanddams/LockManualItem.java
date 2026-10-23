package com.aschuchter21.locksanddams;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
public final class LockManualItem extends Item {
    public LockManualItem(){super(new Properties().stacksTo(1));}
    private static final String[] PAGES={
        "Locks & Dams\nBuilder Manual\n\nBuild the chamber,\ngates and\nplumbing, then\nconnect Create\npower and link\nevery control\nnode.\n\nUse means your\nconfigured",
        "Place/Use key, not\nnecessarily\nright-click.",
        "1. Plan the lock\n\nChoose an upper\nand lower canal.\nMaximum clear\nwidth: 32.\nInterior length:\n127. Lift: 48.\nTotal water depth:\n64.\n\nVolume limit:\n131,072 blocks.",
        "Keep the whole\nlock and machinery\nloaded.",
        "2. Four hinges\n\nPlace two hinges\nacross each end.\nEach pair must\nshare a height.\nUpper hinges may\nbe higher. All\nfour must form a\nrectangle when\nviewed from above.\n\nHinge shafts face",
        "down into the\ndrive.",
        "3. Floor and walls\n\nBuild a solid\nfloor and\nwatertight side\nwalls. The upper\nend needs a solid\nwall beneath its\nraised gates.\n\nKeep the chamber\ninterior clear of\nbuilding blocks.",
        "4. Gate pockets\n\nRecessed lower\ngates swing INTO\nthe chamber. Upper\ngates swing into\nthe UPPER canal.\n\nProvide pockets\nwith solid floors\nand backing walls,\nlong enough for\neach leaf, plus",
        "room for catwalk\nrails.",
        "5. Gate panels\n\nBuild a complete\nrectangular leaf\ndirectly above\neach hinge. Leaves\nmeet at the\nmiddle; on odd\nwidths, one has an\nextra column.\n\nLower gates must\nreach above the",
        "upper water level.",
        "6. Gate assembly\n\nIndividual panels\nbecome continuous\ntimber leaves when\nassembled. Their\nclosed V points\ntoward high water.\n\nLeave one clear\nwater/air row on\nthe high-water\nside. Keep the",
        "gate swing clear.",
        "7. Controller\n\nPlace the\ncontroller IN a\nlong side wall\nbetween the gates,\nabove the floor\nand below upper\nwater level.\n\nKeep it and\nchamber ports\nbeyond the lower",
        "gate pockets. Use\nit to assemble\nafter plumbing is\ncomplete.",
        "8. Fill circuit\n\nUpper canal port\n-> pipes -> ONE\ninline valve ->\nchamber port.\n\nCanal port mouth\nfaces source water\nat the upper\nsurface. Chamber\noutlet faces\ninward below the",
        "upper level. Pipes\nattach to port\nbacks.",
        "9. Drain circuit\n\nChamber port ->\npipes -> ONE\ninline valve ->\nlower canal port.\n\nChamber intake\nfaces inward below\nthe lower surface.\nCanal outlet faces\nsource water at\nthat surface,",
        "beyond the lower\ngate.",
        "10. Pipe rules\n\nEach separate\ncircuit needs\nexactly two ports\nand one valve.\nConnect both valve\nflanges along its\naxis. No bypass\naround the valve.\n\nMaximum: 1,024\nconnected blocks,",
        "256-block reach.",
        "11. Assemble\n\nUse the lock\ncontroller. Follow\nany reported block\ncoordinates or\nmissing\nconnections.\n\nPlayers and boats\nmust clear gate\nopenings. They may\nstay elsewhere in",
        "the chamber. Other\nentities are\nignored.",
        "12. Create power\n\nThe desk supplies\nsignals, NOT\nrotation. Drive\nEVERY hinge\nthrough Create\nmachinery.\n\nPositive input\nopens, negative\ncloses, zero\nholds. Use a",
        "clutch to stop and\na gearshift to\nreverse each\ndrive.",
        "13. Gate nodes\n\nPlace nodes by\nyour clutches and\ngearshifts. Use\nSneak + Use on a\nnode to choose\nUPPER or LOWER.\n\nFront output ->\nclutch: 15 STOPS.\nTop output ->\ngearshift: 15",
        "CLOSES.",
        "14. Link\neverything!\n\nHold the Control\nLink Tool. Use it\non the controller,\nthen the desk.\n\nUse it on EVERY\nGate Control Node\ntoo! Linking only\nthe desk is not\nenough. An",
        "unlinked node\nkeeps its clutch\nstopped.",
        "15. Normal cycle\n\nAt lower level:\nopen lower gates,\nenter, close them.\nSelect FILL.\n\nWait for upper\nlevel, open upper\ngates, exit, then\nclose. Select\nDRAIN to return.\nBoth gate pairs",
        "must close before\nwater moves.",
        "16. Desk lamps\n\nGates: green\nCLOSED, red OPEN,\nflashing yellow\nMOVING.\n\nWater: WARN during\nthe 10-second\ncountdown, FLOW\nduring movement,\nCLOSED when idle.\nFILL/DRAIN has no",
        "OFF position.",
        "17. Alarm and horn\n\nFILL/DRAIN starts\na 10-second\nwarning output\nbefore flow.\n\nThe horn output\npulses for 2\nseconds, 4 seconds\nafter a commanded\ngate fully opens.\nConnect your own",
        "alarm or horn\ndevices.",
        "18. Emergency stop\n\nE-stop stops gates\nand water. Sneak +\nUse the red button\nto reset. Old\ncommands do not\nautomatically\nrestart.\n\nPlayers on\ncatwalks or in\ngate paths stop",
        "movement. Boats in\ngate paths also\nstop it.",
        "19. Gates not\nmoving?\n\nCheck ALL nodes\nare linked and\nassigned to the\ncorrect end. Check\nhinge RPM, clutch\nand gearshift\nwiring.\n\nUpper gates need\nupper water level;",
        "lower gates need\nlower level. Clear\nplayers/boats from\ntheir path.",
        "20. No water flow?\n\nWait for warning\nto finish. Both\ngate pairs must\nreport CLOSED.\nCheck circuit\nports, valve,\nloaded chunks and\ncontroller\nmessage.\n\nFor an example,",
        "run /locks\ndemo_desk in a\nclear creative\ntest area."
    };
    public static ItemStack book(){
        ItemStack stack=new ItemStack(Items.WRITTEN_BOOK);CompoundTag n=stack.getOrCreateTag();
        n.putString("title","Locks & Dams Manual");n.putString("author","Locks & Dams");n.putBoolean("resolved",true);
        ListTag pages=new ListTag();for(String p:PAGES)pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(p))));n.put("pages",pages);return stack;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide){player.setItemInHand(hand,book());player.openItemGui(player.getItemInHand(hand),hand);}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
