package progress.model;

public class Assignment {
    private final int id;
    private final int courseId;
    private final String title;
    private final double maxScore;
    private final String status;
    private Double score;

    public Assignment(int id, int courseId, String title, double maxScore, Double score) {
        this(id, courseId, title, maxScore, score, score != null ? "Graded" : "Pending");
    }

    public Assignment(int id, int courseId, String title, double maxScore, Double score, String status) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.maxScore = maxScore;
        this.score = score;
        this.status = (status == null || status.isBlank())
            ? (score != null ? "Graded" : "Pending")
            : status;
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

    public String getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return score != null;
    }

    public double getScorePercent() {
        if (score == null || maxScore <= 0) {
            return 0.0;
        }
        return (score / maxScore) * 100.0;
    }
}
