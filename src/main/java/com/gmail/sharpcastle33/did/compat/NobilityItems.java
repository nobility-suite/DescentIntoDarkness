package com.gmail.sharpcastle33.did.compat;

import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class NobilityItems {
	private static final boolean nobilityItemsPresent;
	private static final Class<?> cNobilityItems;
	private static final Method mGetNullableItemByName;
	private static final Method mGetItem;
	private static final Method mGetNullableBlockByName;
	private static final Method mGetBlock;
	private static final Class<?> cNobilityItem;
	private static final Method mGetItemStack;
	private static final Method mItemGetInternalName;
	private static final Class<?> cNobilityBlock;
	private static final Method mGetBlockData;
	private static final Method mBlockGetInternalName;

	static {
		boolean _nobilityItemsPresent = true;
		Class<?> _cNobilityItems = null;
		Method _mGetNullableItemByName = null;
		Method _mGetItem = null;
		Method _mGetNullableBlockByName = null;
		Method _mGetBlock = null;
		Class<?> _cNobilityItem = null;
		Method _mGetItemStack = null;
		Method _mItemGetInternalName = null;
		Class<?> _cNobilityBlock = null;
		Method _mGetBlockData = null;
		Method _mBlockGetInternalName = null;
		try {
			_cNobilityItems = Class.forName("net.civex4.nobilityitems.NobilityItems");
		} catch (ClassNotFoundException e) {
			_nobilityItemsPresent = false;
		}
		if (_nobilityItemsPresent) {
			try {
				_mGetNullableItemByName = _cNobilityItems.getMethod("getNullableItemByName", String.class);
				_mGetItem = _cNobilityItems.getMethod("getItem", ItemStack.class);
				_mGetNullableBlockByName = _cNobilityItems.getMethod("getNullableBlockByName", String.class);
				_mGetBlock = _cNobilityItems.getMethod("getBlock", BlockData.class);
				_cNobilityItem = Class.forName("net.civex4.nobilityitems.NobilityItem");
				_mGetItemStack = _cNobilityItem.getMethod("getItemStack", int.class);
				_mItemGetInternalName = _cNobilityItem.getMethod("getInternalName");
				_cNobilityBlock = Class.forName("net.civex4.nobilityitems.NobilityBlock");
				_mGetBlockData = _cNobilityBlock.getMethod("getBlockData");
				_mBlockGetInternalName = _cNobilityBlock.getMethod("getInternalName");
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		}

		nobilityItemsPresent = _nobilityItemsPresent;
		cNobilityItems = _cNobilityItems;
		mGetNullableItemByName = _mGetNullableItemByName;
		mGetItem = _mGetItem;
		mGetNullableBlockByName = _mGetNullableBlockByName;
		mGetBlock = _mGetBlock;
		cNobilityItem = _cNobilityItem;
		mGetItemStack = _mGetItemStack;
		mItemGetInternalName = _mItemGetInternalName;
		cNobilityBlock = _cNobilityBlock;
		mGetBlockData = _mGetBlockData;
		mBlockGetInternalName = _mBlockGetInternalName;
	}

	public static ItemStack createNobilityStack(String name, int amount) {
		if (!nobilityItemsPresent) {
			return null;
		}

		try {
			Object nobilityItem = mGetNullableItemByName.invoke(null, name);
			if (nobilityItem == null) {
				return null;
			}
			return (ItemStack) mGetItemStack.invoke(nobilityItem, amount);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	public static String getNobilityName(ItemStack stack) {
		if (!nobilityItemsPresent) {
			return null;
		}

		try {
			Object nobilityItem = mGetItem.invoke(null, stack);
			if (nobilityItem == null) {
				return null;
			}
			return (String) mItemGetInternalName.invoke(nobilityItem);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	public static BlockData getNobilityBlock(String name) {
		if (!nobilityItemsPresent) {
			return null;
		}

		try {
			Object nobilityBlock = mGetNullableBlockByName.invoke(null, name);
			if (nobilityBlock == null) {
				return null;
			}
			return (BlockData) mGetBlockData.invoke(nobilityBlock);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	public static String getNobilityName(BlockData block) {
		if (!nobilityItemsPresent) {
			return null;
		}

		try {
			Object nobilityBlock = mGetBlock.invoke(null, block);
			if (nobilityBlock == null) {
				return null;
			}
			return (String) mBlockGetInternalName.invoke(nobilityBlock);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

}
