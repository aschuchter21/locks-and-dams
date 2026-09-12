package com.aschuchter21.locksanddams;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class Content {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LocksAndDams.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LocksAndDams.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, LocksAndDams.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LocksAndDams.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LocksAndDams.MOD_ID);
    public static final RegistryObject<Fluid> WATER = FLUIDS.register("chamber_water", ChamberFluid::new);
    public static final RegistryObject<Block> WATER_BLOCK = BLOCKS.register("chamber_water", ChamberWaterBlock::new);
    public static final RegistryObject<Block> CONTROLLER = block("lock_controller", ControllerBlock::new);
    public static final RegistryObject<Block> PANEL = block("gate_panel", GatePanelBlock::new);
    public static final RegistryObject<Block> SEAL = BLOCKS.register("gate_seal", GateSealBlock::new);
    public static final RegistryObject<Block> HINGE = block("lock_hinge", LockHingeBlock::new);
    public static final RegistryObject<Block> PIPE = block("culvert_pipe", () -> new Block(metal()));
    public static final RegistryObject<Block> PORT = block("culvert_port", CulvertPortBlock::new);
    public static final RegistryObject<Block> DRIVE = block("gate_drive", ControlBlock::new);
    public static final RegistryObject<Block> FILL = block("fill_valve", ControlBlock::new);
    public static final RegistryObject<Block> DRAIN = block("drain_valve", ControlBlock::new);
    public static final RegistryObject<BlockEntityType<LockEntity>> LOCK = ENTITIES.register("lock_controller",
        () -> BlockEntityType.Builder.of(LockEntity::new, CONTROLLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<LockHingeEntity>> HINGE_ENTITY = ENTITIES.register("lock_hinge",
        () -> BlockEntityType.Builder.of(LockHingeEntity::new, HINGE.get()).build(null));
    static {
        TABS.register("locks_and_dams", () -> CreativeModeTab.builder()
            .title(Component.literal("Locks & Dams"))
            .icon(() -> new ItemStack(CONTROLLER.get()))
            .displayItems((parameters, output) -> {
                output.accept(CONTROLLER.get()); output.accept(PANEL.get()); output.accept(DRIVE.get());
                output.accept(FILL.get()); output.accept(DRAIN.get());
                output.accept(HINGE.get()); output.accept(PIPE.get()); output.accept(PORT.get());
            }).build());
    }
    private static RegistryObject<Block> block(String name, java.util.function.Supplier<Block> factory) {
        RegistryObject<Block> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static BlockBehaviour.Properties metal() { return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3, 6); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); FLUIDS.register(bus); ENTITIES.register(bus); TABS.register(bus);
    }
}
