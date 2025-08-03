package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RelativePosition { 
    public static final RelativePosition ZERO = new RelativePosition(0, 0, 0);
    public final byte x, y, z;

    public RelativePosition(int x, int y, int z) { 
        if (x < Byte.MIN_VALUE || x > Byte.MAX_VALUE || y < Byte.MIN_VALUE || y > Byte.MAX_VALUE || z < Byte.MIN_VALUE || z > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ", " + z + ")");
        }
        this.x = (byte) x; 
        this.y = (byte) y; 
        this.z = (byte) z; 
    }

    public BlockPos transform(BlockPos pos, Direction direction) {
        int nx = x, nz = z;
        for (int i = direction.get2DDataValue(); i > 0; i--) {
            int temp = nx;
            nx = -nz;
            nz = temp;
        }
        return new BlockPos(pos.getX() + nx, pos.getY() + y, pos.getZ() + nz);
    }

    @Override
    public String toString() { 
        return "(" + x + ", " + y + ", " + z + ")"; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof RelativePosition other) {
            return other.x == x && other.y == y && other.z == z;
        } else return false;
    }

    @Override
    public int hashCode() { 
        return Objects.hash(x, y, z); 
    }

    public static class Suit {
        public final RelativePosition rp;
        public final BlockPos bp;

        public Suit(RelativePosition rp, BlockPos bp) { 
            this.rp = rp; 
            this.bp = bp; 
        }
    }

    public static class Combination {
        public final Set<RelativePosition> positions;

        public Combination(Set<RelativePosition> positions) { 
            this.positions = positions; 
        } 

        public Set<Suit> transform(Direction direction, BlockPos pos) {
            Set<Suit> suits = new HashSet<>();
            for (RelativePosition rp : positions) {
                BlockPos bp = rp.transform(pos, direction);
                suits.add(new Suit(rp, bp));
            }
            return suits;
        }

        public static Combination decode(String src) {
            Set<RelativePosition> positions = new HashSet<>();
            String[] parts = src.split(";");
            for (String part : parts) {
                String[] coords = part.split(",");
                if (coords.length == 3) {
                    byte x = Byte.parseByte(coords[0]);
                    byte y = Byte.parseByte(coords[1]);
                    byte z = Byte.parseByte(coords[2]);
                    positions.add(new RelativePosition(x, y, z));
                }
            }
            positions.add(ZERO);
            return new Combination(Collections.unmodifiableSet(positions));
        }

        public static String encode(Combination combination) {
            StringBuilder sb = new StringBuilder();
            for (RelativePosition pos : combination.positions) {
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(pos.x).append(",").append(pos.y).append(",").append(pos.z);
            }
            return sb.toString();
        }
    }
}