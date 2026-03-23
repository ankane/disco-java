package org.ankane.disco;

/**
 * A coordinate list (COO) matrix.
 */
class CooMatrix {
    // separate vectors to avoid padding
    public int[] rowIndices;
    public int[] colIndices;
    public float[] values;
    int size;

    CooMatrix(int size) {
        this.rowIndices = new int[size];
        this.colIndices = new int[size];
        this.values = new float[size];
        this.size = 0;
    }

    public void add(int rowIndex, int colIndex, float value) {
        this.rowIndices[size] = rowIndex;
        this.colIndices[size] = colIndex;
        this.values[size] = value;
        this.size++;
    }

    public int size() {
        return this.size;
    }
}
