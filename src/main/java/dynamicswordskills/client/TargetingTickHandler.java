/**
    Copyright (C) <2016> <coolAlias>

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

package dynamicswordskills.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.entity.DSSPlayerInfo;
import net.minecraft.client.Minecraft;

/**
 * 
 * A render tick handler for updating the player's facing while locked on to a target.
 *
 */
@SideOnly(Side.CLIENT)
public class TargetingTickHandler
{
	private final Minecraft mc;

	public TargetingTickHandler() {
		this.mc = Minecraft.getMinecraft();
	}

	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		if (event.phase == Phase.START) {
			DSSPlayerInfo skills = (mc.thePlayer == null ? null : DSSPlayerInfo.get(mc.thePlayer));
			if (skills != null) {
				skills.onRenderTick(event.renderTickTime);
				if (skills.swingProgress > 0.0F) {
					mc.thePlayer.swingProgress = skills.swingProgress;
				}
				if (skills.prevSwingProgress > 0.0F) {
					mc.thePlayer.prevSwingProgress = skills.prevSwingProgress;
				}
			}
		}
	}
}
