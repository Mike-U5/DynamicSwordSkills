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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.SyncConfigPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.IComboSkill;
import dynamicswordskills.skills.Skills;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

/**
 * 
 * Event handler for all combat-related events
 *
 */
public class DSSCombatEvents
{
	@SubscribeEvent
	public void giveSkillsOnJoin(EntityJoinWorldEvent event) {
		if (event.entity.dimension == 0 && event.entity instanceof EntityPlayer) {
			final DSSPlayerInfo player = DSSPlayerInfo.get((EntityPlayer)event.entity);
			player.grantSkill(Skills.swordBasic);
			player.grantSkill(Skills.backSlice);
			player.grantSkill(Skills.mortalDraw);
			player.grantSkill(Skills.parry);
			player.grantSkill(Skills.risingCut);
			player.grantSkill(Skills.dash);
			player.grantSkill(Skills.endingBlow);
		}
	}
	
	public static void onAttack() {
//		PlayerUtil.isSword();
	}

	/**
	 * Used for anti-spam of left click, if enabled in the configuration settings.
	 */
	public static void setPlayerAttackTime(EntityPlayer player) {
		DSSPlayerInfo.get(player).setAttackCooldown(Config.getBaseSwingSpeed());
	}

	@SubscribeEvent
	public void onStartItemUse(PlayerUseItemEvent.Start event) {
		if (!DSSPlayerInfo.get(event.entityPlayer).canUseItem()) {
			event.setCanceled(true);
		}
	}

	/**
	 * This event is called when an entity is attacked by another entity; it is only
	 * called on the server unless the source of the attack is an EntityPlayer
	 */
	@SubscribeEvent
	public void onAttacked(LivingAttackEvent event) {
		if (event.source.getEntity() instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.source.getEntity()).onAttack(event);
		}
		if (!event.isCanceled() && event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.entity).onBeingAttacked(event);
		}
	}

	@SubscribeEvent(priority=EventPriority.NORMAL)
	public void onHurt(LivingHurtEvent event) {
		if (event.source.getEntity() instanceof EntityPlayer) {
			final EntityPlayer player = (EntityPlayer) event.source.getEntity();
			
			DSSPlayerInfo.get(player).onImpact(event);
			if (event.ammount <= 0.0F) {
				event.setCanceled(true);
			} else {
				// Nerf base damage (:<
				if (PlayerUtils.isSword(player.getHeldItem())) {
					event.ammount = event.ammount * 0.85F;
				}
			}
		}
	}

	/**
	 * Use LOW or LOWEST priority to prevent interrupting a combo when the event may be canceled elsewhere.
	 */
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onPostHurt(LivingHurtEvent event) {
		if (!event.isCanceled() && event.ammount > 0.0F && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
			if (combo != null && event.ammount > 0) {
				combo.onPlayerHurt(player, event);
			}
		}
		if (!event.isCanceled() && event.ammount > 0.0F && event.source.getEntity() instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.source.getEntity()).onPostImpact(event);
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		if (event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.entity).onUpdate();
		}
	}

	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityPlayer && DSSPlayerInfo.get((EntityPlayer) event.entity) == null) {
			DSSPlayerInfo.register((EntityPlayer) event.entity);
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		DSSPlayerInfo.get(event.player).onPlayerLoggedIn();
		if (event.player instanceof EntityPlayerMP) {
			PacketDispatcher.sendTo(new SyncConfigPacket(), (EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.entity).onJoinWorld();
		}
	}

	@SubscribeEvent
	public void onClonePlayer(PlayerEvent.Clone event) {
		DSSPlayerInfo.get(event.entityPlayer).copy(DSSPlayerInfo.get(event.original));
	}

	/**
	 * NOTE: LivingFallEvent is only called when not in Creative mode
	 */
	@SubscribeEvent
	public void onFall(LivingFallEvent event) {
		if (event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.entity).onFall(event);
		}
	}

	@SubscribeEvent
	public void onCreativeFall(PlayerFlyableFallEvent event) {
		DSSPlayerInfo.get(event.entityPlayer).onCreativeFall(event);
	}
}
