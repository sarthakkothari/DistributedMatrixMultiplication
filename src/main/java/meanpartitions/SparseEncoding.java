package meanpartitions;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class SparseEncoding implements WritableComparable<SparseEncoding>{

    private Integer row;
    private Integer col;
    private Integer val;
    private Character src;

    public SparseEncoding() { }

    /**
     * Takes input as string of the form "<row><col><val>"
     * @param value
     */
    public SparseEncoding(String value) {
        String[] values = value.split(",");
        row = Integer.parseInt(values[0]);
        col = Integer.parseInt(values[1]);
        val = Integer.parseInt(values[2]);
        src = values[3].charAt(0);
    }

    public SparseEncoding(Integer row, Integer col, Integer val, Character src) {
        this.row = row;
        this.col = col;
        this.val = val;
        this.src = src;
    }

    public SparseEncoding(SparseEncoding s) {
        this.row = s.row;
        this.col = s.col;
        this.val = s.val;
        this.src = s.src;
    }

    public SparseEncoding(Integer row, Integer col, Integer val) {
        this.row = row;
        this.col = col;
        this.val = val;
    }

    @Override
    public int compareTo(SparseEncoding that) {
        // compare only on src; so matrix A (left matrix) comes before matrix B (right matrix)
        return this.getSrc().compareTo(that.getSrc());
    }

    @Override
    public void write(DataOutput out) throws IOException {
//        out.writeChars(this.toString());
        out.writeInt(row);
        out.writeInt(col);
        out.writeInt(val);
        out.writeChar(src);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        row = in.readInt();
        col = in.readInt();
        val = in.readInt();
        src = in.readChar();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SparseEncoding that = (SparseEncoding) o;
        return Objects.equals(row, that.row) &&
                Objects.equals(col, that.col) &&
                Objects.equals(val, that.val) &&
                Objects.equals(src, that.src);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col, val, src);
    }

    @Override
    public String toString() {
        return row + "," + col + "," + val + "," + src;
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

    public Integer getVal() {
        return val;
    }

//    public void setVal(Integer val) {
//        this.val = val;
//    }

    public Character getSrc() {
        return src;
    }

    public void setSrc(Character src) {
        this.src = src;
    }
}
