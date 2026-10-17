import java.util.List;

// King piece (OOP: Inheritance from Piece, Method Overriding of moves() to add castling)
class King extends Piece {

    King(Side s) {
        super(s);
    }

    String letter() {
        return "K";
    }

    String name() {
        return "King";
    }

    int kind() {
        return 0;
    }

    int value() {
        return 0;
    }

    Piece copy() {
        return mark(new King(side));
    }

    // King attacks one square in every direction
    List<int[]> attacks(Board b, int r, int c) {
        return step(r, c, ALL_DIRS);
    }

    // Overrides the parent method: normal moves plus castling
    @Override
    List<Move> moves(Board b, int r, int c) {
        List<Move> out = super.moves(b, r, c);
        int home = side == Side.WHITE ? 7 : 0;

        // Castling needs: unmoved king on its home square and not in check
        if (!moved && r == home && c == 4 && !b.isAttacked(r, c, side.opp())) {

            // King-side castling
            Piece k = b.at(r, 7);
            if (k instanceof Rook && k.side == side && !k.moved
                    && b.at(r, 5) == null && b.at(r, 6) == null
                    && !b.isAttacked(r, 5, side.opp()) && !b.isAttacked(r, 6, side.opp())) {
                Move m = new Move(r, c, r, 6);
                m.castle = true;
                out.add(m);
            }

            // Queen-side castling
            Piece q = b.at(r, 0);
            if (q instanceof Rook && q.side == side && !q.moved
                    && b.at(r, 1) == null && b.at(r, 2) == null && b.at(r, 3) == null
                    && !b.isAttacked(r, 3, side.opp()) && !b.isAttacked(r, 2, side.opp())) {
                Move m = new Move(r, c, r, 2);
                m.castle = true;
                out.add(m);
            }
        }
        return out;
    }
}
