package my.cool.projects;

import java.util.Objects;

public class BoardLocation {
    int row;
    int column;
    String chessLingo;

    public BoardLocation(int row, int column) {
        this.row = row;
        this.column = column;
        this.chessLingo = PlayChess.toChessLingo(row, column);
    }

    public BoardLocation(String chessLingo) {
        this.chessLingo = chessLingo;
        this.row = PlayChess.determineMoveToRow(chessLingo);
        this.column = PlayChess.determineMoveToColumn(chessLingo);
    }

    public void setLocation(int row, int column) {
        this.row = row;
        this.column = column;
        this.chessLingo = PlayChess.toChessLingo(row, column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardLocation that = (BoardLocation) o;
        return row == that.row &&
                column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }
}
