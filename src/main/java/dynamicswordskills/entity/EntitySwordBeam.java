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

package dynamicswordskills.entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.Skills;
import dynamicswordskills.skills.SwordBeam;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

/**
 * 
 * Sword beam shot from Link's sword when at full health. Inflicts a portion of
 * the original sword's base damage to the first entity struck, less 20% for each
 * additional target thus struck.
 * 
 * If using the Master Sword, the beam will shoot through enemies, hitting all
 * entities in its direct path.
 *
 */
public class EntitySwordBeam extends EntityThrowable
{
	/** Damage that will be inflicted on impact */
	private float damage = 4.0F;

	/** Skill level of user; affects range */
	private int level = 1;

	/** Base number of ticks this entity can exist */
	private int lifespan = 12;

	public EntitySwordBeam(World world) {
		super(world);
	}

	public EntitySwordBeam(World world, EntityLivingBase entity) {
		super(world, entity);
	}

	public EntitySwordBeam(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	@Override
	public void entityInit() {
		setSize(0.5F, 0.5F);
	}

	/**
	 * Each level increases the distance the beam will travel
	 */
	public EntitySwordBeam setLevel(int level) {
		this.level = level;
		this.lifespan += level;
		return this;
	}

	/**
	 * Sets amount of damage that will be caused onImpact
	 */
	public EntitySwordBeam setDamage(float amount) {
		this.damage = amount;
		return this;
	}

	/** Entity's velocity factor */
	@Override
	protected float func_70182_d() {
		return 1.0F + (level * 0.15F);
	}

	@Override
	public float getGravityVelocity() {
		return 0.0F;
	}

	@Override
	public float getBrightness(float partialTick) {
		return 1.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float partialTick) {
		return 0xf000f0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (inGround || ticksExisted > lifespan) {
			setDead();
		}
		for (int i = 0; i < 2; ++i) {
			worldObj.spawnParticle((i % 2 == 1 ? "magicCrit" : "crit"), posX, posY, posZ, motionX + rand.nextGaussian(), 0.01D, motionZ + rand.nextGaussian());
			worldObj.spawnParticle((i % 2 == 1 ? "magicCrit" : "crit"), posX, posY, posZ, -motionX + rand.nextGaussian(), 0.01D, -motionZ + rand.nextGaussian());
		}
	}

	@Override
	protected void onImpact(MovingObjectPosition mop) {
		if (!worldObj.isRemote) {
			EntityPlayer player = (getThrower() instanceof EntityPlayer ? (EntityPlayer) getThrower() : null);
			SwordBeam skill = (player != null ? (SwordBeam) DSSPlayerInfo.get(player).getPlayerSkill(Skills.swordBeam) : null);
			if (mop.typeOfHit == MovingObjectType.ENTITY) {
				Entity entity = mop.entityHit;
				if (entity == player) { return; }
				if (player != null) {
					if (skill != null) {
						skill.onImpact(player, false);
					}
					if (entity.attackEntityFrom(DamageUtils.causeIndirectComboDamage(this, player).setProjectile(), damage)) {
						PlayerUtils.playSoundAtEntity(worldObj, entity, ModInfo.SOUND_HURT_FLESH, 0.4F, 0.5F);
					}
					damage *= 0.8F;
				}
				if (this.level < Skills.swordBeam.getMaxLevel()) {
					setDead();
				}
			} else {
				Block block = worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
				if (block.getMaterial().blocksMovement()) {
					if (player != null && skill != null) {
						skill.onImpact(player, true);
					}
					setDead();
				}
			}
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setFloat("damage", damage);
		compound.setInteger("level", level);
		compound.setInteger("lifespan", lifespan);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		damage = compound.getFloat("damage");
		level = compound.getInteger("level");
		lifespan = compound.getInteger("lifespan");
	}
}
