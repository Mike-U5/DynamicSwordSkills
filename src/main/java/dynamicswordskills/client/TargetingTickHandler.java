/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

package dynamicswordskills.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.lib.Config;
import dynamicswordskills.skills.Dodge;
import dynamicswordskills.skills.EndingBlow;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.Parry;
import dynamicswordskills.skills.RisingCut;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SpinAttack;
import dynamicswordskills.skills.SwordBreak;
import dynamicswordskills.util.PlayerUtils;

/**
 * 
 * A render tick handler for updating the player's facing while locked on to a target.
 *
 */
@SideOnly(Side.CLIENT)
public class TargetingTickHandler
{
	private final Minecraft mc;

	/** The player whose view will update */
	private EntityPlayer player = null;

	/** Target from player's currently active ILockOnTarget */
	private Entity target = null;

	/** Whether the left movement key has been pressed; used every tick */
	boolean isLeftPressed;

	public TargetingTickHandler() {
		this.mc = Minecraft.getMinecraft();
	}

	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		if (event.phase == Phase.START) {
			player = mc.thePlayer;
			if (player != null && DSSPlayerInfo.get(player) != null) {
				DSSPlayerInfo skills = DSSPlayerInfo.get(player);
				ILockOnTarget skill = skills.getTargetingSkill();
				if (skill != null) {
					// Flag is true if an animation is in progress
					boolean flag = false;

					if (skills.isSkillActive(SkillBase.dodge)) {
						flag = ((Dodge) skills.getPlayerSkill(SkillBase.dodge)).onRenderTick(player);
					} else if (skills.isSkillActive(SkillBase.spinAttack)) {
						flag = ((SpinAttack) skills.getPlayerSkill(SkillBase.spinAttack)).onRenderTick(player);
					} else if (skills.isSkillActive(SkillBase.risingCut)) {
						flag = ((RisingCut) skills.getPlayerSkill(SkillBase.risingCut)).onRenderTick(player);
					}
					if (!flag && skill.isLockedOn()) {
						target = skill.getCurrentTarget();
						updatePlayerView();
					}
					if (skill.isLockedOn() && skills.canInteract()) {
						if (isVanillaKeyPressed(mc.gameSettings.keyBindJump)) {
							if (skills.hasSkill(SkillBase.risingCut) && !skills.isSkillActive(SkillBase.risingCut) && !PlayerUtils.isUsingItem(player) && player.isSneaking()) {
								((RisingCut) skills.getPlayerSkill(SkillBase.risingCut)).keyPressed();
							} else if (skills.hasSkill(SkillBase.leapingBlow) && !skills.isSkillActive(SkillBase.leapingBlow) && PlayerUtils.isUsingItem(player)) {
								skills.activateSkill(mc.theWorld, SkillBase.leapingBlow);
								KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
								KeyBinding.setKeyBindState(DSSKeyHandler.keys[DSSKeyHandler.KEY_BLOCK].getKeyCode(), false);
							}
						} else if (isVanillaKeyPressed(mc.gameSettings.keyBindForward)) {
							if (skills.hasSkill(SkillBase.endingBlow)) {
								((EndingBlow) skills.getPlayerSkill(SkillBase.endingBlow)).keyPressed();
							}
						} else if (Config.allowVanillaControls()) {
							isLeftPressed = isVanillaKeyPressed(mc.gameSettings.keyBindLeft);
							if (isLeftPressed || isVanillaKeyPressed(mc.gameSettings.keyBindRight)) {
								if (skills.hasSkill(SkillBase.spinAttack)) {
									((SpinAttack) skills.getPlayerSkill(SkillBase.spinAttack)).keyPressed((isLeftPressed ? mc.gameSettings.keyBindLeft : mc.gameSettings.keyBindRight), player);
								}
								if (skills.hasSkill(SkillBase.dodge) && player.onGround) {
									((Dodge) skills.getPlayerSkill(SkillBase.dodge)).keyPressed((isLeftPressed ? mc.gameSettings.keyBindLeft : mc.gameSettings.keyBindRight), player);
								}
							} else if (isVanillaKeyPressed(mc.gameSettings.keyBindBack)) {
								if (PlayerUtils.isUsingItem(player) && skills.hasSkill(SkillBase.swordBreak)) {
									((SwordBreak) skills.getPlayerSkill(SkillBase.swordBreak)).keyPressed(player);
								} else if (skills.hasSkill(SkillBase.parry)) {
									((Parry) skills.getPlayerSkill(SkillBase.parry)).keyPressed(player);
								}
							}
						}
					}
				}
			}
		} else {
			this.player = null;
			this.target = null;
		}
	}

	/**
	 * Returns true if a vanilla keybinding is both getIsKeyPressed() and isPressed()
	 * This is necessary to prevent skills from being activated as soon as locking on to a target,
	 * (when isPressed() is still true) or while the key is held down (getIsKeyPressed() is true).
	 */
	@SideOnly(Side.CLIENT)
	private boolean isVanillaKeyPressed(KeyBinding key) {
		return key.isPressed() && key.getIsKeyPressed();
	}

	/**
	 * Rotates the player to face the current target
	 */
	private void updatePlayerView() {
		double dx = player.posX - target.posX;
		double dz = player.posZ - target.posZ;
		double angle = Math.atan2(dz, dx) * 180 / Math.PI;
		double pitch = Math.atan2(player.posY - (target.posY + (target.height / 2.0F)), Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
		double distance = player.getDistanceToEntity(target);
		float rYaw = (float)(angle - player.rotationYaw);
		while (rYaw > 180) { rYaw -= 360; }
		while (rYaw < -180) { rYaw += 360; }
		rYaw += 90F;
		float rPitch = (float) pitch - (float)(10.0F / Math.sqrt(distance)) + (float)(distance * Math.PI / 90);
		player.setAngles(rYaw, -(rPitch - player.rotationPitch));
	}
}
