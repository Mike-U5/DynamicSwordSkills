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

package dynamicswordskills.network.bidirectional;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Send to either side to {@link SkillActive#deactivate deactivate} a skill.
 *
 */
public class DeactivateSkillPacket extends AbstractMessage<DeactivateSkillPacket>
{
	private byte id;

	public DeactivateSkillPacket() {}

	public DeactivateSkillPacket(SkillActive skill) {
		this.id = skill.getId();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		id = buffer.readByte();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(id);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		// handled identically on both sides
		SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(SkillRegistry.getSkillById(id));
		if (skill instanceof SkillActive) {
			((SkillActive) skill).deactivate(player);
		} else {
			DynamicSwordSkills.logger.warn(String.format("Skill ID %d was not valid for %s while processing DeactivateSkillPacket", id, player));
		}
	}
}
