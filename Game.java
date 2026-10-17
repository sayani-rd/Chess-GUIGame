import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Class that controls the game flow: turns, undo, status (OOP: Encapsulation, Composition - Game "has" a Board)
class Game {

    // Enum listing every possible state of the game
    enum Status { PLAYING, CHECKMATE, STALEMATE, DRAW_MATERIAL, DRAW_FIFTY, DRAW_REPETITION }

    // Nested class: saved copy of the game used by undo
    private static class Snap {
        Board board;
        Side turn;
        Move last;
        List<String> notation;
        List<Piece> capW, capB;
        Map<String, Integer> seen;
        Status status;
        Side winner;
        boolean check;
    }

    Board board;
    Side turn;                 // side to move
    Move last;                 // last move played
    Status status;
    Side winner;
    boolean check;
    List<String> notation;     // move list in algebraic notation
    List<Piece> capturedByWhite, capturedByBlack;
    private Map<String, Integer> seen;                      // counts positions for repetition
    private final Deque<Snap> history = new ArrayDeque<>(); // stack of snapshots for undo

    // Constructor: starts a fresh game
    Game() {
        reset();
    }

    // Sets everything back to the starting position
    void reset() {
        board = new Board();
        board.setup();
        turn = Side.WHITE;
        last = null;
        status = Status.PLAYING;
        winner = null;
        check = false;
        notation = new ArrayList<>();
        capturedByWhite = new ArrayList<>();
        capturedByBlack = new ArrayList<>();
        seen = new HashMap<>();
        seen.put(board.key(turn), 1);
        history.clear();
    }

    boolean canUndo() {
        return !history.isEmpty();
    }

    boolean isOver() {
        return status != Status.PLAYING;
    }

    // Saves the current state before a move
    private Snap snapshot() {
        Snap s = new Snap();
        s.board = board.copy();
        s.turn = turn;
        s.last = last;
        s.notation = new ArrayList<>(notation);
        s.capW = new ArrayList<>(capturedByWhite);
        s.capB = new ArrayList<>(capturedByBlack);
        s.seen = new HashMap<>(seen);
        s.status = status;
        s.winner = winner;
        s.check = check;
        return s;
    }

    // Restores the most recent snapshot
    void undo() {
        if (history.isEmpty()) {
            return;
        }
        Snap s = history.pop();
        board = s.board;
        turn = s.turn;
        last = s.last;
        notation = s.notation;
        capturedByWhite = s.capW;
        capturedByBlack = s.capB;
        seen = s.seen;
        status = s.status;
        winner = s.winner;
        check = s.check;
    }

    // Plays a move, then updates turn, check and end-of-game status
    void play(Move m) {
        history.push(snapshot());
        String san = notate(m);
        Piece cap = board.apply(m);
        if (cap != null) {
            (turn == Side.WHITE ? capturedByWhite : capturedByBlack).add(cap);
        }
        turn = turn.opp();
        last = m;
        check = board.inCheck(turn);
        boolean any = board.hasLegal(turn);

        // Decide whether the game has ended
        if (!any) {
            status = check ? Status.CHECKMATE : Status.STALEMATE;
            winner = check ? turn.opp() : null;
        } else if (board.insufficientMaterial()) {
            status = Status.DRAW_MATERIAL;
        } else if (board.halfmove >= 100) {
            status = Status.DRAW_FIFTY;
        } else if (seen.merge(board.key(turn), 1, Integer::sum) >= 3) {
            status = Status.DRAW_REPETITION;
        }

        if (check) {
            san += any ? "+" : "#";
        }
        notation.add(san);
    }

    // Positive = White is ahead in captured material
    int materialBalance() {
        int w = 0, b = 0;
        for (Piece p : capturedByWhite) {
            w += p.value();
        }
        for (Piece p : capturedByBlack) {
            b += p.value();
        }
        return w - b;
    }

    // Text shown in the status box
    String statusText() {
        switch (status) {
            case CHECKMATE:
                return "Checkmate! " + winner.label() + " wins";
            case STALEMATE:
                return "Draw - stalemate";
            case DRAW_MATERIAL:
                return "Draw - insufficient material";
            case DRAW_FIFTY:
                return "Draw - fifty-move rule";
            case DRAW_REPETITION:
                return "Draw - threefold repetition";
            default:
                return turn.label() + " to move" + (check ? " - CHECK!" : "");
        }
    }

    // Converts a move to standard algebraic notation (e.g. Nf3, exd5, O-O)
    private String notate(Move m) {
        if (m.castle) {
            return m.tc > m.fc ? "O-O" : "O-O-O";
        }
        Piece p = board.at(m.fr, m.fc);
        boolean cap = board.at(m.tr, m.tc) != null || m.enPassant;
        StringBuilder sb = new StringBuilder();

        if (p instanceof Pawn) {
            if (cap) {
                sb.append((char) ('a' + m.fc));
            }
        } else {
            sb.append(p.letter());

            // Check if another piece of the same type can reach the same square
            boolean amb = false, sameFile = false, sameRank = false;
            for (Move o : board.allLegal(turn)) {
                if (o.fr == m.fr && o.fc == m.fc) {
                    continue;
                }
                if (o.tr == m.tr && o.tc == m.tc && board.at(o.fr, o.fc).getClass() == p.getClass()) {
                    amb = true;
                    if (o.fc == m.fc) {
                        sameFile = true;
                    }
                    if (o.fr == m.fr) {
                        sameRank = true;
                    }
                }
            }

            // Add file and/or rank to remove the confusion
            if (amb) {
                if (!sameFile) {
                    sb.append((char) ('a' + m.fc));
                } else if (!sameRank) {
                    sb.append(8 - m.fr);
                } else {
                    sb.append((char) ('a' + m.fc)).append(8 - m.fr);
                }
            }
        }

        if (cap) {
            sb.append('x');
        }
        sb.append((char) ('a' + m.tc)).append(8 - m.tr);
        if (m.promo != 0) {
            sb.append('=').append(m.promo);
        }
        return sb.toString();
    }
}
