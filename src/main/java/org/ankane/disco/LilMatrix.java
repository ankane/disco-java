package org.ankane.disco;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of lists (LIL) matrix.
 */
class LilMatrix {
    public List<List<SparseRow>> row_list;

    public LilMatrix() {
        this.row_list = new ArrayList<>();
    }

    public void add(int row_index, int col_index, float value) {
        if (row_index == this.row_list.size()) {
            this.row_list.add(new ArrayList<>());
        }
        this.row_list.get(row_index).add(new SparseRow(col_index, value));
    }
}
