/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.api.WeaponRegistry;
import dynamicswordskills.command.DSSCommands;
import dynamicswordskills.crafting.RecipeInfuseSkillOrb;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION, guiFactory = ModInfo.ID + ".client.gui.GuiFactoryConfig")
public class DynamicSwordSkills
{
	@Mod.Instance(ModInfo.ID)
	public static DynamicSwordSkills instance;

	@SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
	public static CommonProxy proxy;

	public static final Logger logger = LogManager.getLogger(ModInfo.ID);

	/** Expected FPS used as a reference to normalize e.g. client-side motion adjustments */
	public static final float BASE_FPS = 30F;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (Loader.isModLoaded("zeldaswordskills")) {
			throw new RuntimeException("Dynamic Sword Skills may not be loaded at the same time as Zelda Sword Skills! Please remove one or the other.");
		}
		Skills.init();
		Config.init(event);
		EntityRegistry.registerModEntity(EntityLeapingBlow.class, "leapingblow", 0, this, 64, 10, true);
		EntityRegistry.registerModEntity(EntitySwordBeam.class, "swordbeam", 1, this, 64, 10, true);
		PacketDispatcher.initialize();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.registerRenderers();
		DSSCombatEvents events = new DSSCombatEvents();
		MinecraftForge.EVENT_BUS.register(events);
		FMLCommonHandler.instance().bus().register(events); // for PlayerLoggedInEvent
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
		String link = "https://raw.githubusercontent.com/coolAlias/DynamicSwordSkills/master/src/main/resources/versionlist.json";
		FMLInterModComms.sendRuntimeMessage(ModInfo.ID, "VersionChecker", "addVersionCheck", link);
		GameRegistry.addRecipe(new RecipeInfuseSkillOrb());
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Config.postInit();
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		DSSCommands.registerCommands(event);
	}

	@Mod.EventHandler
	public void processMessages(FMLInterModComms.IMCEvent event) {
		for (final FMLInterModComms.IMCMessage msg : event.getMessages()) {
			WeaponRegistry.INSTANCE.processMessage(msg);
		}
	}

	@Mod.EventHandler
	public void processMissingMappings(FMLMissingMappingsEvent event) {
		for (MissingMapping mapping : event.get()) {
			String s = mapping.name.replace(ModInfo.ID + ":", "");
			String location = null;
			if (s.matches("^dss.skillitem([0-9])+$")) {
				int i = Integer.valueOf(s.replace("dss.skillitem", ""));
				SkillBase skill = SkillRegistry.getSkillById(i);
				if (Skills.superSpinAttack.is(skill)) {
					mapping.ignore();
				} else if (skill != null) {
					location = "training_stick_" + skill.getRegistryName().getResourcePath().toLowerCase();
				}
			} else if (s.startsWith("training_stick_")) {
				// Handle skill registry name changes
				String skill_name = s.substring("training_stick_".length());
				SkillBase skill = SkillRegistry.get(new ResourceLocation(ModInfo.ID, skill_name));
				if (Skills.superSpinAttack.is(skill)) {
					mapping.ignore();
				} else if (skill != null && !skill.getRegistryName().getResourcePath().equals(skill_name)) {
					location = "training_stick_" + skill.getRegistryName().getResourcePath().toLowerCase();
				}
			} else if (s.matches("^dss.skill(wood|stone|iron|diamond|gold)$")) {
				location = s.replace("dss.skill", "skill_sword_").toLowerCase();
			} else if (s.startsWith("dss.")) {
				location = s.replace("dss.", "").toLowerCase();
			}
			if (location != null) {
				Item item = GameRegistry.findItem(ModInfo.ID, location);
				if (item == null) {
					mapping.fail();
				} else {
					mapping.remap(item);
				}
			}
		};
	}

	/**
	 * Adds weighted chest contents to all ChestGenHooks, with possible exception of Bonus Chest
	 */
	private void addLootToAll(WeightedRandomChestContent loot, boolean bonus) {
		ChestGenHooks.getInfo(ChestGenHooks.MINESHAFT_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_DESERT_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_JUNGLE_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_LIBRARY).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CROSSING).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.VILLAGE_BLACKSMITH).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST).addItem(loot);
		if (bonus) {
			ChestGenHooks.getInfo(ChestGenHooks.BONUS_CHEST).addItem(loot);
		}
	}

	/**
	 * Parses a String into a ResourceLocation, or NULL if format was invalid
	 * @param name A valid ResourceLocation string e.g. 'modid:registry_name'
	 */
	public static ResourceLocation getResourceLocation(String name) {
		try {
			return new ResourceLocation(name);
		} catch (NullPointerException e) {
			DynamicSwordSkills.logger.error(String.format("Invalid ResourceLocation string: %s", name));
		}
		return null;
	}
}
