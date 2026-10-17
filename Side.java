// Enum that represents the two players (OOP: Enumeration / fixed set of constants with methods)
enum Side {
    WHITE, BLACK;

    // Returns the opposite side
    Side opp() {
        return this == WHITE ? BLACK : WHITE;
    }

    // Returns a readable name for display
    String label() {
        return this == WHITE ? "White" : "Black";
    }
}
