package meanpartitions;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class SimpleKey implements WritableComparable<SimpleKey> {
    private Integer row;
    private Integer col;

    public SimpleKey() {
    }

    public SimpleKey(Integer row, Integer col) {
        this.row = row;
        this.col = col;
    }

    @Override // TODO: think of secondary sort optimization
    public int compareTo(SimpleKey o) {
        int cmp = compare(row, o.row);
        if (cmp != 0) {
            return cmp;
        }
        return compare(col, o.col);
    }

    public static int compare(int a, int b) {
        return (a < b ? -1 : (a == b ? 0 : 1));
    }

    @Override
    public void write(DataOutput out) throws IOException {
//        out.writeChars(this.toString());
        out.writeInt(row);
        out.writeInt(col);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        row = in.readInt();
        col = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleKey simpleKey = (SimpleKey) o;
        return Objects.equals(row, simpleKey.row) &&
                Objects.equals(col, simpleKey.col);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return row + "," + col;
    }

    // getters & setters

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getCol() {
        return col;
    }

    public void setCol(Integer col) {
        this.col = col;
    }
}
