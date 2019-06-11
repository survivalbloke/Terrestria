package net.coderbot.terrestria.feature;

import com.mojang.datafixers.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableIntBoundingBox;
import net.minecraft.world.ModifiableTestableWorld;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.AbstractTreeFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;

import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class RubberTreeFeature extends AbstractTreeFeature<DefaultFeatureConfig> {
	private BlockState wood;
	private BlockState leaves;

	public RubberTreeFeature(Function<Dynamic<?>, ? extends DefaultFeatureConfig> function, boolean notify, TreeDefinition.Basic tree) {
		super(function, notify);

		this.wood = tree.wood;
		this.leaves = tree.leaves;
	}

	@Override
	public boolean generate(Set<BlockPos> blocks, ModifiableTestableWorld world, Random rand, BlockPos origin, MutableIntBoundingBox boundingBox) {
		// Total trunk height
		int height = rand.nextInt(4) + 8;

		// How much "bare trunk" there will be.
		int bareTrunkHeight = 1 + rand.nextInt(12);

		// Maximum leaf radius.
		int maxRadius = 2 + rand.nextInt(6);

		if(origin.getY() + height + 1 > 256 || origin.getY() < 1) {
			return false;
		}

		BlockPos below = origin.down();

		if(!isNaturalDirtOrGrass(world, below)) {
			return false;
		}

		if(!checkForObstructions(world, origin, height, bareTrunkHeight, maxRadius)) {
			return false;
		}

		setBlockState(blocks, world, origin.down(), Blocks.DIRT.getDefaultState(), boundingBox);
		growTrunk(blocks, world, new BlockPos.Mutable(origin), height, boundingBox);
		growBranches(blocks, world, new BlockPos.Mutable(origin), height, rand, boundingBox);

		return true;
	}

	// TODO: This is still the conifer tree code...
	private boolean checkForObstructions(TestableWorld world, BlockPos origin, int height, int bareTrunkHeight, int radius) {
		BlockPos.Mutable pos = new BlockPos.Mutable(origin);

		for(int i = 0; i < bareTrunkHeight; i++) {
			if(!canTreeReplace(world, pos.setOffset(Direction.UP))) {
				return false;
			}
		}

		for(int dY = bareTrunkHeight; dY < height; dY++) {
			for(int dZ = -radius; dZ <= radius; dZ++) {
				for(int dX = -radius; dX <= radius; dX++) {
					pos.set(origin.getX() + dX, origin.getY() + dY, origin.getZ() + dZ);
					
					if(!canTreeReplace(world, pos)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	// Grows a 3x3 trunk of the specified height, centered on the position.
	private void growTrunk(Set<BlockPos> blocks, ModifiableTestableWorld world, BlockPos.Mutable pos, int height, MutableIntBoundingBox boundingBox) {
		int x = pos.getX() - 1;
		int z = pos.getZ() - 1;

		for(int i = 0; i < height; i++) {
			for(int dx = 0; dx < 3; dx++) {
				for(int dz = 0; dz < 3; dz++) {
					pos.set(x + dx, pos.getY(), z + dz);
					setBlockState(blocks, world, pos, wood, boundingBox);
				}
			}

			pos.setOffset(Direction.UP);
		}

		for(int dx = 0; dx < 3; dx++) {
			for(int dz = 0; dz < 3; dz++) {
				pos.set(x + dx, pos.getY(), z + dz);
				setBlockState(blocks, world, pos, leaves, boundingBox);
			}
		}
	}

	private void growBranches(Set<BlockPos> blocks, ModifiableTestableWorld world, BlockPos.Mutable pos, int height, Random random, MutableIntBoundingBox boundingBox) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		for(int branch = 0; branch < 32; branch++) {
			int baseY = random.nextInt(height - 4) + 4;

			float length = random.nextFloat() * 7 + 2;
			float angle = random.nextFloat() * (float)Math.PI * 2;

			int offsetX = (int)(MathHelper.cos(angle) * length);
			int offsetZ = (int)(MathHelper.sin(angle) * length);

			int moveX = offsetX > 0 ? 1 : -1;
			int moveZ = offsetZ > 0 ? 1 : -1;

			int absX = Math.abs(offsetX);
			int absZ = Math.abs(offsetZ);

			int movedX = 0;
			int movedZ = 0;

			float stepX = 1.0F / absX;
			float stepZ = 1.0F / absZ;

			for(int movement = 0; movement < absX + absZ; movement++) {
				if(Math.abs(movedX * stepX) < Math.abs(movedZ * stepZ) && Math.abs(movedX) < absX) {
					movedX += moveX;
				} else {
					movedZ += moveZ;
				}

				int offsetY = (int)(Math.sqrt(movedX*movedX + movedZ * movedZ) * 0.4);

				if(movedX > 6 || movedX < -6 || movedZ > 6 || movedZ < -6) {
					continue;
				}

				pos.set(x + movedX, y + baseY + offsetY, z + movedZ);

				if(!canTreeReplace(world, pos)) {
					break;
				}

				//if(random.nextInt(3) == 0 || (movedX == offsetX && movedZ == offsetZ))
				setBlockState(blocks, world, pos, wood, boundingBox);

				for(Direction direction: Direction.values()) {
					pos.set(x + movedX, y + baseY + offsetY, z + movedZ);
					pos.setOffset(direction);

					if(AbstractTreeFeature.isAirOrLeaves(world, pos)) {
						setBlockState(blocks, world, pos, leaves, boundingBox);
					}
				}
			}
		}
	}
}