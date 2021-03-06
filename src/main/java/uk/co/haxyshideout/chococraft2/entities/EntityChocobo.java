package uk.co.haxyshideout.chococraft2.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.AnimalChest;
import net.minecraft.inventory.IInvBasic;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.lwjgl.input.Keyboard;
import uk.co.haxyshideout.chococraft2.ChocoCraft2;
import uk.co.haxyshideout.chococraft2.config.Additions;
import uk.co.haxyshideout.chococraft2.config.Constants;
import uk.co.haxyshideout.chococraft2.entities.ai.ChocoboAIAvoidPlayer;
import uk.co.haxyshideout.chococraft2.entities.ai.ChocoboAIFollowLure;
import uk.co.haxyshideout.chococraft2.entities.ai.ChocoboAIFollowOwner;
import uk.co.haxyshideout.chococraft2.entities.ai.ChocoboAIHealInPen;
import uk.co.haxyshideout.chococraft2.entities.ai.ChocoboAIWatchPlayer;
import uk.co.haxyshideout.haxylib.utils.InventoryHelper;
import uk.co.haxyshideout.haxylib.utils.RandomHelper;
import uk.co.haxyshideout.haxylib.utils.WorldHelper;

import javax.annotation.Nullable;

/**
 * Created by clienthax on 14/4/2015.
 */
public class EntityChocobo extends EntityTameable implements IInvBasic
{
	public float wingRotation;
	public float destPos;
	private float wingRotDelta;
	private EntityPlayerMP entityLuring = null;
	private ChocoboAIAvoidPlayer chocoboAIAvoidPlayer;
	private ChocoboAIHealInPen chocoboAIHealInPen;
	private AnimalChest chocoboChest;
	private int timeUntilNextFeatherDrop;
	private final RiderState riderState;

	public enum ChocoboColor
	{
		YELLOW,
		GREEN,
		BLUE,
		WHITE,
		BLACK,
		GOLD,
		PINK,
		RED,
		PURPLE
	}

	public enum BagType
	{
		NONE, SADDLE, PACK
	}

	public enum MovementType
	{
		WANDER, FOLLOW_OWNER, STANDSTILL, FOLLOW_LURE
	}

	public EntityChocobo(World world)
	{
		super(world);
		this.setSize(1.3f, 2.3f);
		setMale(world.rand.nextBoolean());
		setCustomNameTag(DefaultNames.getRandomName(isMale()));
		resetFeatherDropTime();
		riderState = new RiderState();
		((PathNavigateGround) this.getNavigator()).setAvoidsWater(true);
		this.tasks.addTask(1, new EntityAIWander(this, 1.0D));
		this.tasks.addTask(1, new ChocoboAIFollowOwner(this, 1.0D, 5.0F, 5.0F));// follow speed 1, min and max 5
		this.tasks.addTask(1, new ChocoboAIFollowLure(this, 1.0D, 5.0F, 5.0F));
		this.tasks.addTask(1, new ChocoboAIWatchPlayer(this, EntityPlayer.class, 5));

		initChest();

		// Default init
		if (!world.isRemote)
		{
			if (WorldHelper.isHellWorld(world))
				setColor(ChocoboColor.PURPLE);
			else
				setColor(ChocoboColor.YELLOW);
		}
	}

	@Override
	public void mountEntity(Entity entityIn)
	{
		super.mountEntity(entityIn);

		if (this.worldObj.isRemote)
		{
			if (Minecraft.getMinecraft().thePlayer.getUniqueID().equals(entityIn.getUniqueID()))
			{
				Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
			}
		}
	}

	public void setStats()
	{
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.getAbilityInfo().getMaxHP());// set max health to 30
		setHealth(getMaxHealth());// reset the hp to max
		onGroundSpeedFactor = this.getAbilityInfo().getLandSpeed() / 100f;

	}

	public RiderState getRiderState()
	{
		return riderState;
	}

	@Override
	public void moveEntityWithHeading(float strafe, float forward)
	{
		// TODO some point in future, move this to its own ai class
		if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityLivingBase)
		{
			this.prevRotationYaw = this.rotationYaw = this.riddenByEntity.rotationYaw;
			this.rotationPitch = this.riddenByEntity.rotationPitch * 0.5F;
			this.setRotation(this.rotationYaw, this.rotationPitch);
			this.rotationYawHead = this.renderYawOffset = this.rotationYaw;
			strafe = ((EntityLivingBase) this.riddenByEntity).moveStrafing * 0.5F;
			forward = ((EntityLivingBase) this.riddenByEntity).moveForward;

			if (forward <= 0.0F)
			{
				forward *= 0.25F;
			}

			if (this.riderState.isJumping() && this.getAbilityInfo().getCanFly())
			{
				this.isJumping = true;
				this.jump();
			}
			else if (this.riderState.isJumping() && !this.isJumping && this.onGround)
			{
				this.motionY += 0.75;
				this.riderState.setJumping(false);
				this.isJumping = true;
			}

			if (!this.worldObj.isRemote)
			{
				this.setAIMoveSpeed((float) this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
				super.moveEntityWithHeading(strafe, forward);
			}

			this.prevLimbSwingAmount = this.limbSwingAmount;
			double d1 = this.posX - this.prevPosX;
			double d0 = this.posZ - this.prevPosZ;
			float f4 = MathHelper.sqrt_double(d1 * d1 + d0 * d0) * 4.0F;

			if (f4 > 1.0F)
			{
				f4 = 1.0F;
			}

			this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
			this.limbSwing += this.limbSwingAmount;
		}
		else
			super.moveEntityWithHeading(strafe, forward);
	}

	@Override
	public void setupTamedAI()
	{
		if (chocoboAIAvoidPlayer == null)
		{
			chocoboAIAvoidPlayer = new ChocoboAIAvoidPlayer(this, 10.0F, 1.0D, 1.2D);
		}
		if (chocoboAIHealInPen == null)
		{
			chocoboAIHealInPen = new ChocoboAIHealInPen(this);
		}

		tasks.removeTask(chocoboAIAvoidPlayer);
		tasks.removeTask(chocoboAIHealInPen);

		if (isTamed())
		{
			tasks.addTask(4, chocoboAIHealInPen);
		}
		else
		{
			tasks.addTask(5, chocoboAIAvoidPlayer);
		}
	}

	@Override
	public void onLivingUpdate()
	{
		super.onLivingUpdate();

		if (this.getAbilityInfo().canClimb())
		{
			this.stepHeight = 1.0F;
		}

		this.fallDistance = 0f;

		// Wing rotations, control packet, client side
		if (worldObj.isRemote)
		{
			// Client side
			if (riddenByEntity != null && riddenByEntity instanceof EntityPlayer)
			{
				if (Minecraft.getMinecraft().thePlayer.getUniqueID().equals(riddenByEntity.getUniqueID()) && Keyboard.isKeyDown(Keyboard.KEY_SPACE))
				{
					this.riderState.setJumping(true);
				}

				ChocoCraft2.proxy.updateRiderState((EntityPlayer) riddenByEntity);
			}

			this.destPos += (double) (this.onGround ? -1 : 4) * 0.3D;
			this.destPos = MathHelper.clamp_float(destPos, 0f, 1f);

			if (!this.onGround)
			{
				this.wingRotDelta = Math.min(wingRotation, 1f);
			}

			this.wingRotDelta *= 0.9D;

			if (!this.onGround && this.motionY < 0.0D)
			{
				this.motionY *= 0.8D;
			}
			this.wingRotation += this.wingRotDelta * 2.0F;

			return;// Rest of code should be run on server only
		}

		// Drop a chocobo feather randomly
		if (--timeUntilNextFeatherDrop <= 0)
		{
			// TODO config
			if (RandomHelper.getChanceResult(80))
				entityDropItem(new ItemStack(Additions.chocoboFeatherItem), 0);
			resetFeatherDropTime();
		}

		if (riddenByEntity != null)
		{
			// TODO is this needed?
			rotationPitch = 0;
			rotationYaw = riddenByEntity.rotationYaw;
			prevRotationYaw = riddenByEntity.rotationYaw;
			setRotation(rotationYaw, rotationPitch);
		}

	}

	public void resetFeatherDropTime()
	{
		timeUntilNextFeatherDrop = rand.nextInt(500);// TODO config
	}

	/*
	 * DataWatcher
	 */

	@Override
	public void writeEntityToNBT(NBTTagCompound tagCompound)
	{
		super.writeEntityToNBT(tagCompound);
		tagCompound.setByte("Color", (byte) getChocoboColor().ordinal());
		tagCompound.setByte("BagType", (byte) getBagType().ordinal());
		tagCompound.setBoolean("Saddled", isSaddled());
		tagCompound.setBoolean("Male", isMale());
		tagCompound.setByte("MovementType", (byte) getMovementType().ordinal());

		if (getBagType() != BagType.NONE)
		{
			NBTTagList nbttaglist = new NBTTagList();

			for (int i = 0; i < chocoboChest.getSizeInventory(); ++i)
			{
				ItemStack itemstack = chocoboChest.getStackInSlot(i);

				if (itemstack != null)
				{
					NBTTagCompound nbttagcompound1 = new NBTTagCompound();
					nbttagcompound1.setByte("Slot", (byte) i);
					itemstack.writeToNBT(nbttagcompound1);
					nbttaglist.appendTag(nbttagcompound1);
				}
			}

			tagCompound.setTag("Items", nbttaglist);
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tagCompound)
	{
		super.readEntityFromNBT(tagCompound);
		setColor(ChocoboColor.values()[tagCompound.getByte("Color")]);
		setBag(BagType.values()[tagCompound.getByte("BagType")]);
		setSaddled(tagCompound.getBoolean("Saddled"));
		setMale(tagCompound.getBoolean("Male"));
		setMovementType(MovementType.values()[tagCompound.getByte("MovementType")]);

		if (getBagType() != BagType.NONE)
		{
			NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
			initChest();

			for (int i = 0; i < nbttaglist.tagCount(); ++i)
			{
				NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
				int j = nbttagcompound1.getByte("Slot") & 255;

				if (j >= 0 && j < chocoboChest.getSizeInventory())
				{
					chocoboChest.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbttagcompound1));
				}
			}
		}

	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		// corresponding to enum.ordinal
		this.dataWatcher.addObject(Constants.dataWatcherVariant, (byte) 0);
		// 0 for no bag, 1 for saddlebag, 2 for pack bag
		this.dataWatcher.addObject(Constants.dataWatcherBagType, (byte) 0);
		// 1 if saddled, 0 if not
		this.dataWatcher.addObject(Constants.dataWatcherSaddled, (byte) 0);
		// 1 if male, 0 if not
		this.dataWatcher.addObject(Constants.dataWatcherMale, (byte) 0);
		// 0 if wandering, 1 if following owner, 2 staying still
		this.dataWatcher.addObject(Constants.dataWatcherFollowingOwner, (byte) 0);
	}

	public void setColor(ChocoboColor color)
	{
		dataWatcher.updateObject(Constants.dataWatcherVariant, (byte) color.ordinal());
		setStats();
	}

	public ChocoboColor getChocoboColor()
	{
		return ChocoboColor.values()[dataWatcher.getWatchableObjectByte(Constants.dataWatcherVariant)];
	}

	public void setBag(BagType bag)
	{
		dataWatcher.updateObject(Constants.dataWatcherBagType, (byte) bag.ordinal());
		initChest();
	}

	public BagType getBagType()
	{
		return BagType.values()[dataWatcher.getWatchableObjectByte(Constants.dataWatcherBagType)];
	}

	public void setSaddled(boolean saddled)
	{
		dataWatcher.updateObject(Constants.dataWatcherSaddled, (byte) (saddled ? 1 : 0));
	}

	public boolean isSaddled()
	{
		return dataWatcher.getWatchableObjectByte(Constants.dataWatcherSaddled) == 1;
	}

	public void setMale(boolean isMale)
	{
		dataWatcher.updateObject(Constants.dataWatcherMale, (byte) (isMale ? 1 : 0));
	}

	public boolean isMale()
	{
		return dataWatcher.getWatchableObjectByte(Constants.dataWatcherMale) == 1;
	}

	public void setMovementType(MovementType movementType)
	{
		dataWatcher.updateObject(Constants.dataWatcherFollowingOwner, (byte) movementType.ordinal());
	}

	public MovementType getMovementType()
	{
		return MovementType.values()[dataWatcher.getWatchableObjectByte(Constants.dataWatcherFollowingOwner)];
	}

	/*
	 * End of dataWatcher
	 */

	@Override
	public EntityAgeable createChild(EntityAgeable ageable)
	{
		if (!this.worldObj.isRemote)
		{
			EntityBabyChocobo entity = new EntityBabyChocobo(ageable.worldObj);
			entity.setColor(this.getChocoboColor());
			entity.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			this.worldObj.spawnEntityInWorld(entity);
			return entity;
		}

		return null;
	}

	@Override
	protected String getDeathSound()
	{
		return "chococraft2:choco_kweh";
	}

	@Override
	protected String getHurtSound()
	{
		return "chococraft2:choco_kweh";
	}

	@Override
	protected String getLivingSound()
	{
		if (rand.nextInt(4) == 0)
			return "chococraft2:choco_kweh";

		return null;
	}

	@Override
	public void heal(float healAmount)
	{
		super.heal(healAmount);
		((WorldServer) worldObj).spawnParticle(EnumParticleTypes.HEART, false, posX, posY + 2.5, posZ, 3, 0.3d, 0, 0.3d, 1);
	}

	@Override
	public double getMountedYOffset()
	{
		return 1.65D;
	}

	@Override
	public boolean shouldRiderFaceForward(EntityPlayer player)
	{
		return true;
	}

	/**
	 * Get number of ticks, at least during which the living entity will be silent.
	 */
	@Override
	public int getTalkInterval()
	{
		return 150;
	}

	/**
	 * Only show the name tag if the chocobo is tamed and not being ridden
	 */
	@Override
	public boolean getAlwaysRenderNameTagForRender()
	{
		return (isTamed() && riddenByEntity == null);
	}

	@Override
	public boolean interact(EntityPlayer player)
	{
		if (!worldObj.isRemote)
			if (player.getHeldItem() != null && player.getHeldItem().getItem() == Additions.chocopediaItem && isTamed() && getOwner() == player)
			{
				ChocoCraft2.proxy.openChocopedia(this);
				return true;
			}

		if (worldObj.isRemote)// return if client
			return false;

		if (player.getHeldItem() == null && isSaddled() && !player.isSneaking())
		{// If the player is not holding anything and the chocobo is saddled, mount the chocobo
			player.mountEntity(this);
			return true;
		}

		if (player.isSneaking() && getBagType() != BagType.NONE)
		{
			player.displayGUIChest(chocoboChest);
		}

		if (player.getHeldItem() == null)// Make sure the player is holding something for the following checks
			return false;

		if (player.getHeldItem().getItem() == Additions.gysahlGreenItem)
		{// random chance of taming + random healing amount
			if (!isTamed() && RandomHelper.getChanceResult(10))
			{
				setOwnerId(player.getUniqueID().toString());
				setTamed(true);
				InventoryHelper.giveIfMissing(new ItemStack(Additions.chocopediaItem), (EntityPlayerMP) player);
				player.addChatComponentMessage(new ChatComponentText("You tamed the chocobo!"));
			}
			else if (isTamed())
			{
				heal(RandomHelper.getRandomInt(5));
			}
			return true;
		}
		else if (player.getHeldItem().getItem() == Additions.gysahlGoldenItem)
		{
			this.createChild(this);
		}

		/*
		 * Follow the player who clicked the entity if not tamed, if the chocobo is tamed, verify this is the owner, and then follow, if the entity is already following the player, stop following them.
		 */
		if (player.getHeldItem().getItem() == Additions.chocoboFeatherItem)
		{
			if (getMovementType() == MovementType.FOLLOW_LURE && entityLuring != null && entityLuring == player)
			{
				setMovementType(MovementType.STANDSTILL);
				entityLuring = null;
				return true;
			}
			if ((isTamed() && getOwnerId().equals(player.getUniqueID().toString())) || !isTamed())
			{
				setMovementType(MovementType.FOLLOW_LURE);
				entityLuring = (EntityPlayerMP) player;
			}
			return true;
		}

		if (!isTamed() || getOwner() != player)// Return if the chocobo is not tamed, as the following require that the chocobo is tamed. and that they are being used by the owner
			return false;

		if (player.getHeldItem().getItem() == Additions.chocoboSaddleItem && !isSaddled())
		{
			// if the player is holding a saddle and the chocobo is not saddled, saddle the chocobo
			player.addChatComponentMessage(new ChatComponentText("Put saddle on Chocobo"));
			setSaddled(true);
			return true;
		}

		if (player.getHeldItem().getItem() == Additions.chocoboSaddleBagItem && getBagType() == BagType.NONE && isSaddled())
		{// holding a saddle bag and no bag on chocobo, chocobo needs to be saddled
			setBag(BagType.SADDLE);
			return true;
		}

		if (player.getHeldItem().getItem() == Additions.chocoboPackBagItem && getBagType() == BagType.NONE)
		{// holding a pack bag and no bag on chocobo, remove the saddle if the chocobo is saddled
			setBag(BagType.PACK);
			return true;
		}

		return false;
	}

	@Override
	public float getJumpUpwardsMotion()
	{
		return 0.5f;
	}

	public EntityPlayerMP getEntityLuring()
	{
		return entityLuring;
	}

	@Override
	protected boolean isMovementBlocked()
	{
		return getMovementType() == MovementType.STANDSTILL && riddenByEntity == null;
	}

	@Override
	public void onDeath(DamageSource cause)
	{
		super.onDeath(cause);
		dropGear(null);
		for (int i = 0; i < rand.nextInt(3); i++)
			entityDropItem(new ItemStack(Additions.chocoboFeatherItem), 0);
	}

	// Inv stuff

	public void initChest()
	{
		AnimalChest animalChest = chocoboChest;
		chocoboChest = new AnimalChest("ChocoboChest", getChestSize());
		chocoboChest.setCustomName(getCustomNameTag());

		if (animalChest != null)
		{
			animalChest.func_110132_b(this);
			int i = Math.min(animalChest.getSizeInventory(), this.chocoboChest.getSizeInventory());
			for (int j = 0; j < i; ++j)
			{
				ItemStack itemstack = animalChest.getStackInSlot(j);
				if (itemstack != null)
				{
					this.chocoboChest.setInventorySlotContents(j, itemstack.copy());
				}
			}
		}
		this.chocoboChest.func_110134_a(this);
	}

	public int getChestSize()
	{
		if (getBagType() == BagType.NONE)
			return 0;
		if (getBagType() == BagType.PACK)
			return 54;
		if (getBagType() == BagType.SADDLE)
			return 27;
		return 0;
	}

	@Override
	public void onInventoryChanged(InventoryBasic inventoryBasic)
	{

	}

	public void dropGear(@Nullable EntityPlayerMP player)
	{
		if (worldObj.isRemote)
			return;

		if (chocoboChest != null)
		{
			for (int slot = 0; slot < chocoboChest.getSizeInventory(); slot++)
			{
				ItemStack itemStack = chocoboChest.getStackInSlot(slot);
				if (itemStack != null)
				{
					if (player != null)
						InventoryHelper.giveOrDropStack(itemStack, player);
					else
						entityDropItem(itemStack, 0);
				}
			}
		}
		if (isSaddled())
		{
			setSaddled(false);
			if (player != null)
				InventoryHelper.giveOrDropStack(new ItemStack(Additions.chocoboSaddleItem), player);
			else
				entityDropItem(new ItemStack(Additions.chocoboSaddleItem), 0);
		}
		if (getBagType() == EntityChocobo.BagType.SADDLE)
		{
			setBag(EntityChocobo.BagType.NONE);
			if (player != null)
				InventoryHelper.giveOrDropStack(new ItemStack(Additions.chocoboSaddleBagItem), player);
			else
				entityDropItem(new ItemStack(Additions.chocoboSaddleBagItem), 0);
		}
		if (getBagType() == EntityChocobo.BagType.PACK)
		{
			setBag(EntityChocobo.BagType.NONE);
			if (player != null)
				InventoryHelper.giveOrDropStack(new ItemStack(Additions.chocoboPackBagItem), player);
			else
				entityDropItem(new ItemStack(Additions.chocoboPackBagItem), 0);
		}
	}

	public ChocoboAbilityInfo getAbilityInfo()
	{
		return ChocoboAbilityInfo.getAbilityInfo(this.getChocoboColor());
	}
}
