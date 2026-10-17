import java.util.List;

// Bishop piece (OOP: Inheritance from Piece, Polymorphism through its own attacks() rule)
class Bishop extends Piece {

    Bishop(Side s) {
        super(s);
    }

    String letter() {
        return "B";
    }

    String name() {
        return "Bishop";
    }

    int kind() {
        return 3;
    }

    int value() {
        return 3;
    }

    Piece copy() {
        return mark(new Bishop(side));
    }

    // Bishop slides any distance diagonally
    List<int[]> attacks(Board b, int r, int c) {
        return slide(b, r, c, BISHOP_DIRS);
    }
}
