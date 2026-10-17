import java.util.List;

// Queen piece (OOP: Inheritance from Piece, Polymorphism through its own attacks() rule)
class Queen extends Piece {

    Queen(Side s) {
        super(s);
    }

    String letter() {
        return "Q";
    }

    String name() {
        return "Queen";
    }

    int kind() {
        return 1;
    }

    int value() {
        return 9;
    }

    Piece copy() {
        return mark(new Queen(side));
    }

    // Queen slides any distance in a straight line or diagonal
    List<int[]> attacks(Board b, int r, int c) {
        return slide(b, r, c, ALL_DIRS);
    }
}
