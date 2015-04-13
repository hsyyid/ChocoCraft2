package uk.co.haxyshideout.chococraft2.config;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import uk.co.haxyshideout.chococraft2.blocks.GysahlStemBlock;
import uk.co.haxyshideout.chococraft2.blocks.StrawBlock;
import uk.co.haxyshideout.haxylib.items.GenericItem;
import uk.co.haxyshideout.haxylib.items.GenericItemFood;
import uk.co.haxyshideout.haxylib.items.GenericItemSeeds;
import uk.co.haxyshideout.haxylib.utils.RegistryHelper;

/**
 * Created by clienthax on 12/4/2015.
 * Should contain every block and item that is added from the mod.
 */
public class Additions {

	public static Block gysahlStemBlock;
	public static Block strawBlock;

	public static Item gysahlGreenItem;
	public static Item gysahlSeedsItem;
	public static Item gysahlGoldenItem;
	public static Item gysahlLoverlyItem;
	public static Item gysahlPinkItem;
	public static Item gysahlRedItem;
	public static Item gysahlRawPicklesItem;
	public static Item gysahlCookedPicklesItem;
	public static Item gysahlCakeItem;
	public static Item gysahlChibiItem;//I have no idea what this is for.

	public static Item chocoboFeatherItem;
	public static Item chocoboLegRawItem;
	public static Item chocoboLegCookedItem;
	public static Item chocopediaItem;
	public static Item purpleSpawnEggItem;
	public static Item chocoboSaddleItem;
	public static Item chocoboSaddleBagItem;
	public static Item chocoboPackBagItem;
	public static Item chocoboWhistleItem;

	//Register items and blocks etc in here
	public static void registerAdditions() {
		gysahlStemBlock = new GysahlStemBlock().setUnlocalizedName("gysahlStemBlock");
		strawBlock = new StrawBlock().setUnlocalizedName("strawBlock").setCreativeTab(ChocoCreativeTabs.chococraft2);

		gysahlGreenItem = new GenericItem().setUnlocalizedName("gysahlGreenItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlSeedsItem = new GenericItemSeeds(gysahlStemBlock, Blocks.farmland).setUnlocalizedName("gysahlSeedsItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlGoldenItem = new GenericItem().setUnlocalizedName("gysahlGoldenItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlLoverlyItem = new GenericItem().setUnlocalizedName("gysahlLoverlyItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlPinkItem = new GenericItem().setUnlocalizedName("gysahlPinkItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlRedItem = new GenericItem().setUnlocalizedName("gysahlRedItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlRawPicklesItem = new GenericItem().setUnlocalizedName("gysahlRawPicklesItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlCookedPicklesItem = new GenericItem().setUnlocalizedName("gysahlCookedPicklesItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlCakeItem = new GenericItem().setUnlocalizedName("gysahlCakeItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		gysahlChibiItem = new GenericItem().setUnlocalizedName("gysahlChibiItem").setCreativeTab(ChocoCreativeTabs.chococraft2);

		chocoboFeatherItem = new GenericItem().setUnlocalizedName("chocoboFeatherItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboLegRawItem = new GenericItemFood(2, 0.3F, true).setPotionEffect(Potion.hunger.getId(), 30, 0, 0.3F).setUnlocalizedName("chocoboLegRawItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboLegCookedItem = new GenericItemFood(6, 0.6F, true).setUnlocalizedName("chocoboLegCookedItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocopediaItem = new GenericItem().setUnlocalizedName("chocopediaItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		purpleSpawnEggItem = new GenericItem().setUnlocalizedName("purpleSpawnEggItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboSaddleItem = new GenericItem().setUnlocalizedName("chocoboSaddleItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboSaddleBagItem = new GenericItem().setUnlocalizedName("chocoboSaddleBagItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboPackBagItem = new GenericItem().setUnlocalizedName("chocoboPackBagItem").setCreativeTab(ChocoCreativeTabs.chococraft2);
		chocoboWhistleItem = new GenericItem().setUnlocalizedName("chocoboWhistleItem").setCreativeTab(ChocoCreativeTabs.chococraft2);

		Minecraft.getMinecraft().currentScreen.getClass()
		Minecraft.getMinecraft().gameSettings.guiScale = 2;
		RegistryHelper.registerFieldsWithGameRegistry(Constants.MODID, Additions.class);
	}


}
