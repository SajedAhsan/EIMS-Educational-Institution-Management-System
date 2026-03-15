package progress.model;

public class Course {
    private final int id;
    private final String code;
    private final String title;

    public Course(int id, String code, String title) {
        this.id = id;
        this.code = code;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return code + " - " + title;
    }
}
