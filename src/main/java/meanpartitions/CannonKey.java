package meanpartitions;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class CannonKey implements WritableComparable<CannonKey> {

    private Integer partitionRow;
    private Integer partitionCol;

    public CannonKey() { }

    public CannonKey(Pair<Integer, Integer> key) {
        partitionRow = key.getKey();
        partitionCol = key.getValue();
    }

    public CannonKey(Integer partitionRow, Integer partitionCol) {
        this.partitionRow = partitionRow;
        this.partitionCol = partitionCol;
    }

    @Override
    public int compareTo(CannonKey that) {
        int cmp = Integer.compare(this.partitionRow, that.partitionRow);
        if (cmp == 0) {
            return Integer.compare(this.partitionCol, that.partitionCol);
        }
        return cmp;
    }

    @Override
    public void write(DataOutput out) throws IOException {
//        out.writeChars(this.toString());
        out.writeInt(partitionRow);
        out.writeInt(partitionCol);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        partitionRow = in.readInt();
        partitionCol = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CannonKey cannonKey = (CannonKey) o;
        return Objects.equals(partitionRow, cannonKey.partitionRow) &&
                Objects.equals(partitionCol, cannonKey.partitionCol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionRow, partitionCol);
    }

    @Override
    public String toString() {
        return partitionRow + "," + partitionCol;
    }

    // getters & setters
    public Integer getPartitionRow() {
        return partitionRow;
    }

    public void setPartitionRow(Integer partitionRow) {
        this.partitionRow = partitionRow;
    }

    public Integer getPartitionCol() {
        return partitionCol;
    }

    public void setPartitionCol(Integer partitionCol) {
        this.partitionCol = partitionCol;
    }
}
