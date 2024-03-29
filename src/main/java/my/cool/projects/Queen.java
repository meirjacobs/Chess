package my.cool.projects;

public class Queen extends Piece {

    public Queen(Color color, BoardLocation boardLocation) {
        super(color, boardLocation);
        pieceType = PieceType.QUEEN;
        value = 9;

    }

    @Override
    public boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        if(!validateInput(board, currentRow, currentColumn, moveToRow, moveToColumn)) return false;
        if(currentColumn == moveToColumn && currentRow == moveToRow) {
            if(printErrors) System.err.println("You have not moved the Queen");
            return false;
        }
        if(!capture) {
            if(board[moveToRow][moveToColumn] != null) {
                if(printErrors) System.err.println("Move-to square is already occupied");
                return false;
            }
        }
        else {
            if(board[moveToRow][moveToColumn] == null) {
                if(printErrors) System.err.printf("No piece to capture on [%d, %d]\n", moveToRow, moveToColumn);
                return false;
            }
            if(board[moveToRow][moveToColumn] != null && board[moveToRow][moveToColumn].color.equals(board[currentRow][currentColumn].color)) {
                if(printErrors) System.err.println("Cannot capture your own piece");
                return false;
            }
        }
        if((moveToColumn != currentColumn && moveToRow != currentRow) && (Math.abs(moveToRow - currentRow) != Math.abs(moveToColumn - currentColumn))) {
            if(printErrors) System.err.println("Queen must move up-down or left-right or diagonally");
            return false;
        }
        int deltaX = moveToRow - currentRow;
        int deltaY = moveToColumn - currentColumn;
        if(Math.abs(deltaX) == Math.abs(deltaY)) { // diagonal movement
            int rowDirection = (deltaX > 0) ? 1 : -1;
            int columnDirection = (deltaY > 0) ? 1 : -1;
            currentRow += rowDirection;
            currentColumn += columnDirection;
            for(; currentRow != moveToRow && currentColumn != moveToColumn;) {
                if(board[currentRow][currentColumn] != null) {
                    if(printErrors) System.err.println("Cannot move Queen because there is a piece in the way of the destination");
                    return false;
                }
                currentRow += rowDirection;
                currentColumn += columnDirection;
            }
        }
        else { // vertical or lateral movement
            int direction;
            if(deltaX != 0) { // vertical
                direction = (moveToRow - currentRow > 0) ? 1 : -1;
                currentRow += direction;
                for(; currentRow != moveToRow; currentRow += direction) {
                    if(board[currentRow][currentColumn] != null) {
                        if(printErrors) System.err.println("Cannot move Queen because there is a piece in the way of the destination");
                        return false;
                    }
                }
            }
            else { // lateral
                direction = (moveToColumn - currentColumn > 0) ? 1 : -1;
                currentColumn += direction;
                for(; currentColumn != moveToColumn; currentColumn += direction) {
                    if(board[currentRow][currentColumn] != null) {
                        if(printErrors) System.err.println("Cannot move Queen because there is a piece in the way of the destination");
                        return false;
                    }
                }
            }
        }
        return true;
    }
}