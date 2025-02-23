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

package dynamicswordskills.network.client;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractClientMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Synchronizes all PlayerInfo data on the client
 *
 */
public class SyncPlayerInfoPacket extends AbstractClientMessage<SyncPlayerInfoPacket>
{
	/** NBTTagCompound used to store and transfer the Player's Info */
	private NBTTagCompound compound;

	/** Whether skills should validate; only false when skills reset */
	private boolean validate = true;

	public SyncPlayerInfoPacket() {}

	public SyncPlayerInfoPacket(DSSPlayerInfo info) {
		compound = new NBTTagCompound();
		info.saveNBTData(compound);
	}

	/**
	 * Sets validate to false for reset skills packets
	 */
	public SyncPlayerInfoPacket setReset() {
		validate = false;
		return this;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		compound = buffer.readNBTTagCompoundFromBuffer();
		validate = buffer.readBoolean();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeNBTTagCompoundToBuffer(compound);
		buffer.writeBoolean(validate);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo info = DSSPlayerInfo.get(player);
		info.loadNBTData(compound);
		if (validate) {
			info.validateSkills();
		}
	}
}
