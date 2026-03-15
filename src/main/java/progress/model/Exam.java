package progress.model;

public class Exam {
    private final int id;
    private final int courseId;
    private final String title;
    private final double maxScore;
    private Double score;

    public Exam(int id, int courseId, String title, double maxScore, Double score) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.maxScore = maxScore;
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public boolean isTaken() {
        return score != null;
    }

    public double getScorePercent() {
        if (score == null || maxScore <= 0) {
            return 0.0;
        }
        return (score / maxScore) * 100.0;
    }
}
