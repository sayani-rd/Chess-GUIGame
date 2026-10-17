import java.util.ArrayList;
import java.util.List;

// Class that represents the 8x8 board (OOP: Encapsulation of board state, Composition - Board "has" Pieces)
class Board {
    final Piece[][] g = new Piece[8][8];   // row 0 = rank 8, row 7 = rank 1
    int epRow = -1, epCol = -1;            // en passant target square
    int halfmove = 0;                      // for the fifty-move rule

    // Checks whether a square is inside the board
    static boolean in(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    // Returns the piece on a square (or null if empty)
    Piece at(int r, int c) {
        return g[r][c];
    }

    // Places all pieces in their starting positions
    void setup() {
        for (Side s : Side.values()) {
            int back = s == Side.WHITE ? 7 : 0, pawn = s == Side.WHITE ? 6 : 1;
            // Polymorphism: all pieces are stored as the parent type Piece
            Piece[] row = {new Rook(s), new Knight(s), new Bishop(s), new Queen(s),
                           new King(s), new Bishop(s), new Knight(s), new Rook(s)};
            for (int c = 0; c < 8; c++) {
                g[back][c] = row[c];
                g[pawn][c] = new Pawn(s);
            }
        }
    }

    // Makes a deep copy of the board (used to test moves safely)
    Board copy() {
        Board b = new Board();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (g[r][c] != null) {
                    b.g[r][c] = g[r][c].copy();
                }
            }
        }
        b.epRow = epRow;
        b.epCol = epCol;
        b.halfmove = halfmove;
        return b;
    }

    // Checks if a square is attacked by the given side
    boolean isAttacked(int r, int c, Side by) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = g[i][j];
                if (p != null && p.side == by) {
                    // Polymorphism: each piece type gives its own attacks()
                    for (int[] t : p.attacks(this, i, j)) {
                        if (t[0] == r && t[1] == c) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Finds the king of the given side
    int[] findKing(Side s) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (g[r][c] instanceof King && g[r][c].side == s) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // Is the king of this side in check?
    boolean inCheck(Side s) {
        int[] k = findKing(s);
        return k != null && isAttacked(k[0], k[1], s.opp());
    }

    // Fully legal moves for the piece on (r,c)
    List<Move> legalMoves(int r, int c) {
        List<Move> out = new ArrayList<>();
        Piece p = g[r][c];
        if (p == null) {
            return out;
        }
        for (Move m : p.moves(this, r, c)) {
            // Try the move on a copy; keep it only if the own king stays safe
            Board t = copy();
            t.apply(m);
            if (!t.inCheck(p.side)) {
                out.add(m);
            }
        }
        return out;
    }

    // All legal moves of one side
    List<Move> allLegal(Side s) {
        List<Move> out = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (g[r][c] != null && g[r][c].side == s) {
                    out.addAll(legalMoves(r, c));
                }
            }
        }
        return out;
    }

    // Does this side have at least one legal move?
    boolean hasLegal(Side s) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (g[r][c] != null && g[r][c].side == s && !legalMoves(r, c).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Applies a move (assumed legal). Returns the captured piece, if any
    Piece apply(Move m) {
        Piece p = g[m.fr][m.fc];
        Piece cap = g[m.tr][m.tc];

        // En passant: the captured pawn is beside the moving pawn
        if (m.enPassant) {
            cap = g[m.fr][m.tc];
            g[m.fr][m.tc] = null;
        }

        // Castling: also move the rook
        if (m.castle) {
            int rf = m.tc > m.fc ? 7 : 0, rt = m.tc > m.fc ? m.tc - 1 : m.tc + 1;
            Piece rook = g[m.fr][rf];
            g[m.fr][rt] = rook;
            g[m.fr][rf] = null;
            rook.moved = true;
        }

        // Normal move
        g[m.tr][m.tc] = p;
        g[m.fr][m.fc] = null;
        p.moved = true;

        // Pawn promotion
        if (m.promo != 0) {
            g[m.tr][m.tc] = Piece.promote(m.promo, p.side);
        }

        // Remember the en passant square after a two-step pawn move
        if (p instanceof Pawn && Math.abs(m.tr - m.fr) == 2) {
            epRow = (m.fr + m.tr) / 2;
            epCol = m.fc;
        } else {
            epRow = -1;
            epCol = -1;
        }

        // Count moves since last capture or pawn move (fifty-move rule)
        halfmove = (p instanceof Pawn || cap != null) ? 0 : halfmove + 1;
        return cap;
    }

    // Detects draws where neither side can possibly checkmate
    boolean insufficientMaterial() {
        int knights = 0, bishops = 0, colour = -1;
        boolean sameColour = true;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = g[r][c];
                if (p == null || p instanceof King) {
                    continue;
                }
                if (p instanceof Knight) {
                    knights++;
                } else if (p instanceof Bishop) {
                    bishops++;
                    int col = (r + c) % 2;
                    if (colour == -1) {
                        colour = col;
                    } else if (colour != col) {
                        sameColour = false;
                    }
                } else {
                    return false;       // pawn, rook or queen can still mate
                }
            }
        }
        if (knights == 0) {
            return sameColour;          // K, K+B(s) on one colour
        }
        return knights == 1 && bishops == 0;   // K+N vs K
    }

    // Position identity text for threefold repetition
    String key(Side turn) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = g[r][c];
                if (p == null) {
                    sb.append('.');
                    continue;
                }
                String l = p.letter().isEmpty() ? "P" : p.letter();
                sb.append(p.side == Side.WHITE ? l : l.toLowerCase());
                if ((p instanceof King || p instanceof Rook) && p.moved) {
                    sb.append('m');
                }
            }
        }
        return sb.append(turn).append(epRow).append(epCol).toString();
    }
}
