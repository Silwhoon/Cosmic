package server.partyquest.pyramid;

public enum PyramidMode {
    EASY(0),
    NORMAL(1),
    HARD(2),
    HELL(3);

    final int mode;

    PyramidMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}