package me.vekster.lightanticheat.util.detection.specific;

import org.bukkit.block.Block;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

final class BlockArraySet extends AbstractSet<Block> {

    private static final Set<Block> EMPTY = Collections.emptySet();

    private final Block[] blocks;
    private final int size;

    private BlockArraySet(final Block[] blocks, final int size) {
        this.blocks = blocks;
        this.size = size;
    }

    static Builder builder(final int capacity) {
        return new Builder(Math.max(0, capacity));
    }

    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Block next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return blocks[index++];
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(final Object object) {
        for (int index = 0; index < size; index++) {
            if (blocks[index].equals(object)) {
                return true;
            }
        }
        return false;
    }

    static final class Builder {
        private Block[] blocks;
        private int size;

        private Builder(final int capacity) {
            this.blocks = new Block[Math.max(1, capacity)];
        }

        void add(final Block block) {
            if (block == null || contains(block)) {
                return;
            }
            if (size == blocks.length) {
                final Block[] resized = new Block[blocks.length * 2];
                System.arraycopy(blocks, 0, resized, 0, blocks.length);
                blocks = resized;
            }
            blocks[size++] = block;
        }

        Set<Block> build() {
            if (size == 0) {
                return EMPTY;
            }
            final Block[] exact = new Block[size];
            System.arraycopy(blocks, 0, exact, 0, size);
            return new BlockArraySet(exact, size);
        }

        private boolean contains(final Block block) {
            for (int index = 0; index < size; index++) {
                if (blocks[index].equals(block)) {
                    return true;
                }
            }
            return false;
        }
    }
}
