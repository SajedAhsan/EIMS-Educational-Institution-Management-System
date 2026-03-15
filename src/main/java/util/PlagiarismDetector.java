package util;

import database.DatabaseManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for detecting plagiarism between student submissions.
 *
 * Algorithm: Jaccard similarity on normalized word sets.
 *   similarity = |A ∩ B| / |A ∪ B|
 *
 * If similarity >= PLAGIARISM_THRESHOLD (70 %), both submissions are flagged
 * in the database and participants are notified via the existing notification system.
 */
public class PlagiarismDetector {

    /** Minimum Jaccard similarity to flag a pair of submissions as plagiarised. */
    private static final double PLAGIARISM_THRESHOLD = 0.70;

    // --------------------------------------------------------------------- //
    //  Text processing helpers                                                //
    // --------------------------------------------------------------------- //

    /**
     * Normalize text for comparison:
     * <ol>
     *   <li>Lower-case</li>
     *   <li>Replace every non-alphanumeric character with a space</li>
     *   <li>Collapse consecutive whitespace to a single space</li>
     *   <li>Trim leading/trailing whitespace</li>
     * </ol>
     */
    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * Compute Jaccard similarity between two already-normalized strings.
     *
     * @return value in [0.0, 1.0] — 1.0 means identical word sets.
     */
    public static double jaccardSimilarity(String normA, String normB) {
        if (normA.isEmpty() && normB.isEmpty()) return 1.0;
        if (normA.isEmpty() || normB.isEmpty()) return 0.0;

        Set<String> setA = new HashSet<>(Arrays.asList(normA.split(" ")));
        Set<String> setB = new HashSet<>(Arrays.asList(normB.split(" ")));

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Read a file from {@code filePath} and return its normalised content.
     * Tries UTF-8 first, then ISO-8859-1 as a fallback for non-UTF-8 sources.
     * Returns an empty string if the file cannot be read (missing, binary, etc.).
     */
    public static String readAndNormalize(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            return normalize(content);
        } catch (IOException e) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                String content = new String(bytes, StandardCharsets.ISO_8859_1);
                return normalize(content);
            } catch (IOException ex) {
                return "";
            }
        }
    }

    // --------------------------------------------------------------------- //
    //  Main entry point                                                       //
    // --------------------------------------------------------------------- //

    /**
     * Run the plagiarism check for a newly submitted file.
     *
     * <p>Steps:
     * <ol>
     *   <li>Normalize the new file's content.</li>
     *   <li>Compare it against every other submission for the same assignment.</li>
     *   <li>Find the highest-similarity peer above the threshold.</li>
     *   <li>If found: mark both submissions in the DB, notify both students and the teacher.</li>
     *   <li>If not found: mark the new submission clean (plagiarized = false).</li>
     * </ol>
     *
     * <p>This method is intentionally non-throwing for routine I/O issues — callers
     * should catch {@link SQLException} for database failures only.
     *
     * @param dbManager    active {@link DatabaseManager} instance
     * @param assetId      ID of the assignment
     * @param newStudentId ID of the student who just submitted
     * @param newFilePath  relative path to the submitted file
     * @param assetTitle   human-readable assignment title (used in notifications)
     * @param groupName    group name (used in the teacher notification)
     * @param teacherId    teacher's user ID, or -1 if unknown
     */
    public static void checkAndMark(DatabaseManager dbManager,
                                    int assetId,
                                    int newStudentId,
                                    String newFilePath,
                                    String assetTitle,
                                    String groupName,
                                    int teacherId) throws SQLException {

        String newContent = readAndNormalize(newFilePath);
        if (newContent.isEmpty()) {
            // Cannot meaningfully compare binary or unreadable files; skip silently.
            return;
        }

        // Retrieve the new submission's DB record (created moments ago by addSubmission).
        DatabaseManager.SubmissionData newSub =
            dbManager.getSubmissionByStudentAndAsset(newStudentId, assetId);
        if (newSub == null) return;

        // Scan all other submissions for this assignment to find the best match.
        List<DatabaseManager.SubmissionData> others = dbManager.getSubmissionsByAsset(assetId);

        double maxSimilarity = 0.0;
        DatabaseManager.SubmissionData bestMatch = null;

        for (DatabaseManager.SubmissionData other : others) {
            if (other.getStudentId() == newStudentId) continue; // skip self

            String otherContent = readAndNormalize(other.getFilePath());
            if (otherContent.isEmpty()) continue;

            double similarity = jaccardSimilarity(newContent, otherContent);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = other;
            }
        }

        if (bestMatch != null && maxSimilarity >= PLAGIARISM_THRESHOLD) {
            int pct = (int) Math.round(maxSimilarity * 100);

            // Persist flags for both submissions.
            dbManager.updatePlagiarismResult(newSub.getId(), true, maxSimilarity, bestMatch.getId());
            dbManager.updatePlagiarismResult(bestMatch.getId(), true, maxSimilarity, newSub.getId());

            String warningMsg = String.format(
                "Warning: Your submission for \"%s\" has high similarity with another submission (%d%%). " +
                "The instructor may review this.",
                assetTitle, pct);

            // Notify both students.
            dbManager.createNotification(newStudentId, "STUDENT", warningMsg);
            dbManager.createNotification(bestMatch.getStudentId(), "STUDENT", warningMsg);

            // Notify the teacher.
            if (teacherId > 0) {
                String teacherMsg = String.format(
                    "Possible plagiarism detected in assignment \"%s\" (group \"%s\") " +
                    "between %s and %s (%d%% similarity).",
                    assetTitle, groupName,
                    newSub.getStudentName(), bestMatch.getStudentName(), pct);
                dbManager.createNotification(teacherId, "TEACHER", teacherMsg);
            }

        } else {
            // No plagiarism found — mark the new submission clean.
            dbManager.updatePlagiarismResult(newSub.getId(), false, 0.0, -1);
        }
    }
}
