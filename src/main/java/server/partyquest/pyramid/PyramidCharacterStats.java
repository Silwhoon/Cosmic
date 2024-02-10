package server.partyquest.pyramid;

public class PyramidCharacterStats {

    private final PyramidDifficulty difficulty;
    private int totalHits;
    private int totalMisses;
    private int totalCools;
    private PyramidRank rank = PyramidRank.C;
    private int exp;

    public PyramidCharacterStats(PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.totalHits = 0;
        this.totalMisses = 0;
        this.totalCools = 0;
    }

    public PyramidDifficulty getDifficulty() {
        return difficulty;
    }

    public PyramidRank getRank() {
        return rank;
    }

    public void setRank(PyramidRank rank) {
        this.rank = rank;
    }

    public void calculateRank() {
        if (rank.equals(PyramidRank.D)) {
            return;
        }

        int totalScore = (totalHits + totalCools - totalMisses);
        if (totalScore >= 3000) rank = PyramidRank.S;
        else if (totalScore >= 2000) rank = PyramidRank.A;
        else if (totalScore >= 1000) rank = PyramidRank.B;
        else rank = PyramidRank.C;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void addHits(int amount) {
        this.totalHits += amount;
    }

    public int getTotalMisses() {
        return totalMisses;
    }

    public void addMisses(int amount) {
        this.totalMisses += amount;
    }

    public int getTotalCools() {
        return totalCools;
    }

    public void addCools(int amount) {
        this.totalCools += amount;
    }

    public int calculateExp() {
        int exp = (totalHits * 20) + (totalCools * 100);
        if (this.rank.equals(PyramidRank.S)) exp += (5500 * this.difficulty.getMode());
        if (this.rank.equals(PyramidRank.A)) exp += (5000 * this.difficulty.getMode());
        if (this.rank.equals(PyramidRank.B)) exp += (4250 * this.difficulty.getMode());
        if (this.rank.equals(PyramidRank.C)) exp += (2000 * this.difficulty.getMode());
        if (this.rank.equals(PyramidRank.D)) exp /= 5;

        return exp;
    }
}
