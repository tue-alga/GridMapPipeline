package arrange.model.util;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Matrix<E> {

    private final E[][] data;

    @SuppressWarnings("unchecked")
    public Matrix(int rows, int columns) {
        data = (E[][]) new Object[rows][columns];
    }

    public Matrix(int rows, int columns, E value) {
        this(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                data[i][j] = value;
            }
        }
    }

    public E get(int row, int column) {
        return data[row][column];
    }

    public E get(Identifier idRow, Identifier idColumn) {
        return data[idRow.getId()][idColumn.getId()];
    }

    public void set(int row, int column, E value) {
        data[row][column] = value;
    }

    public void set(Identifier idRow, Identifier idColumn, E value) {
        data[idRow.getId()][idColumn.getId()] = value;
    }
}
