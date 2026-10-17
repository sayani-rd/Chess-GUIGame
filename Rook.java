import java.util.List;

// Rook piece (OOP: Inheritance from Piece, Polymorphism through its own attacks() rule)
class Rook extends Piece {

    Rook(Side s) {
        super(s);
    }

    String letter() {
        return "R";
    }

    String name() {
        return "Rook";
    }

    int kind() {
        return 2;
    }

    int value() {
        return 5;
    }

    Piece copy() {
        return mark(new Rook(side));
    }

    // Rook slides any distance up, down, left or right
    List<int[]> attacks(Board b, int r, int c) {
        return slide(b, r, c, ROOK_DIRS);
    }
}
