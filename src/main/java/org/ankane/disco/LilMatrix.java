package org.ankane.disco;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of lists (LIL) matrix.
 */
class LilMatrix {
    public List<List<SparseRow>> rowList;

    public LilMatrix() {
        this.rowList = new ArrayList<>();
    }

    public void add(int rowIndex, int colIndex, float value) {
        if (rowIndex == this.rowList.size()) {
            this.rowList.add(new ArrayList<>());
        }
        this.rowList.get(rowIndex).add(new SparseRow(colIndex, value));
    }
}
