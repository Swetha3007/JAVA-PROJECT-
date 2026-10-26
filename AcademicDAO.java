import java.sql.*;
import java.util.*;

/** SQL CRUD + KEC R2024 evaluation engine. */
public class AcademicDAO {

    /** One evaluated course result for a student. */
    public static class Result {
        String roll, name, dept, code, title, type;
        int credits;
        double attPct;
        Double cia1, cia2, asg, ese;
        boolean absent, condoned;
        double scaledCia, scaledEse, total;
        String grade = "-", status = "PENDING";
        int gp;
        boolean pending;
    }

    // ---------------- Data entry ----------------
    public boolean addStudent(String roll, String name, String dept, int sem) {
        String sql = "INSERT INTO students(roll_no,name,department,current_semester) VALUES(?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, roll); p.setString(2, name); p.setString(3, dept.toUpperCase()); p.setInt(4, sem);
            return p.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); return false; }
    }

    public boolean addCourse(String code, String title, int credits, String type, int sem) {
        String sql = "INSERT INTO courses(course_code,title,credits,course_type,semester) VALUES(?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, code); p.setString(2, title); p.setInt(3, credits); p.setString(4, type); p.setInt(5, sem);
            return p.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); return false; }
    }

    /** Enrolls (or updates attendance of) a student; returns enrollment id or -1. */
    public int enroll(String roll, String course, int sem, int total, int attended) {
        String up = "INSERT INTO enrollments(roll_no,course_code,semester,total_classes,attended_classes) VALUES(?,?,?,?,?) "
                  + "ON DUPLICATE KEY UPDATE total_classes=VALUES(total_classes), attended_classes=VALUES(attended_classes)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(up)) {
            p.setString(1, roll); p.setString(2, course); p.setInt(3, sem); p.setInt(4, total); p.setInt(5, attended);
            p.executeUpdate();
            int id = getEnrollmentId(roll, course, sem);
            try (PreparedStatement m = c.prepareStatement("INSERT IGNORE INTO assessment_marks(enrollment_id) VALUES(?)")) {
                m.setInt(1, id); m.executeUpdate();
            }
            return id;
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); return -1; }
    }

    public int getEnrollmentId(String roll, String course, int sem) {
        String sql = "SELECT enrollment_id FROM enrollments WHERE roll_no=? AND course_code=? AND semester=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, roll); p.setString(2, course); p.setInt(3, sem);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : -1; }
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); return -1; }
    }

    public boolean setCondonation(int enrollmentId, boolean approved) {
        return update("UPDATE enrollments SET is_condoned=? WHERE enrollment_id=?", approved ? 1 : 0, enrollmentId);
    }

    // ---------------- Incremental marks ----------------
    public boolean recordPhase1Marks(int id, double cia1, double cia2) {
        if (!valid(cia1) || !valid(cia2)) return false;
        return update("UPDATE assessment_marks SET cia1_marks=?, cia2_marks=? WHERE enrollment_id=?", cia1, cia2, id);
    }

    public boolean recordPhase2Marks(int id, double asg) {
        if (!valid(asg)) return false;
        return update("UPDATE assessment_marks SET assignment_marks=? WHERE enrollment_id=?", asg, id);
    }

    public boolean recordPhase3Marks(int id, Double ese, boolean absent) {
        if (absent) return update("UPDATE assessment_marks SET ese_marks=NULL, is_ese_absent=1 WHERE enrollment_id=?", id);
        if (ese == null || !valid(ese)) return false;
        return update("UPDATE assessment_marks SET ese_marks=?, is_ese_absent=0 WHERE enrollment_id=?", ese, id);
    }

    private boolean valid(double m) {
        if (m < 0 || m > 100) { System.out.println("[ERROR] Marks must be between 0 and 100."); return false; }
        return true;
    }

    private boolean update(String sql, Object... args) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); return false; }
    }

    // ---------------- Evaluation engine (KEC R2024) ----------------
    /** Applies attendance rules, weightage, ESE threshold and grading. Expects r.attPct to be set. */
    public static void evaluate(Result r) {
        // Attendance filter: <65 -> SA; 65-74.9 needs medical condonation; >=75 eligible
        if (r.attPct < 65 || (r.attPct < 75 && !r.condoned)) { r.grade = "SA"; r.gp = 0; r.status = "SA"; return; }
        if (r.absent) { r.grade = "AB"; r.gp = 0; r.status = "AB"; return; }
        if (r.cia1 == null || r.cia2 == null || r.asg == null || r.ese == null) { r.pending = true; return; }

        double ciaW, eseW, eseMinPct;
        switch (r.type) {
            case "PRACTICAL": ciaW = 0.60; eseW = 0.40; eseMinPct = 50; break;
            case "EMBEDDED":  ciaW = 0.50; eseW = 0.50; eseMinPct = 45; break;
            default:          ciaW = 0.40; eseW = 0.60; eseMinPct = 45; break; // THEORY
        }
        r.scaledCia = (r.cia1 + r.cia2 + r.asg) / 3.0 * ciaW;
        r.scaledEse = r.ese * eseW;
        r.total = r.scaledCia + r.scaledEse;

        boolean pass = r.total >= 50 && r.ese >= eseMinPct;
        long t = Math.round(r.total);
        if (!pass)        { r.grade = "RA"; r.gp = 0; r.status = "FAIL"; }
        else if (t >= 91) { r.grade = "O";  r.gp = 10; r.status = "PASS"; }
        else if (t >= 81) { r.grade = "A+"; r.gp = 9;  r.status = "PASS"; }
        else if (t >= 71) { r.grade = "A";  r.gp = 8;  r.status = "PASS"; }
        else if (t >= 61) { r.grade = "B+"; r.gp = 7;  r.status = "PASS"; }
        else              { r.grade = "B";  r.gp = 6;  r.status = "PASS"; }
    }

    /** SGPA = sum(credits * GP) / sum(all enrolled credits). */
    public static double sgpa(List<Result> list) {
        int cr = 0, pts = 0;
        for (Result r : list) { cr += r.credits; pts += r.credits * r.gp; }
        return cr == 0 ? 0 : (double) pts / cr;
    }

    // ---------------- Loading ----------------
    /** roll == null -> all students of the semester. */
    public List<Result> getResults(String roll, int sem) {
        String sql = "SELECT s.roll_no,s.name,s.department,c.course_code,c.title,c.credits,c.course_type,"
                + "e.total_classes,e.attended_classes,e.is_condoned,m.cia1_marks,m.cia2_marks,m.assignment_marks,"
                + "m.ese_marks,m.is_ese_absent FROM enrollments e "
                + "JOIN students s ON s.roll_no=e.roll_no JOIN courses c ON c.course_code=e.course_code "
                + "LEFT JOIN assessment_marks m ON m.enrollment_id=e.enrollment_id WHERE e.semester=? "
                + (roll != null ? "AND e.roll_no=? " : "") + "ORDER BY s.roll_no,c.course_code";
        List<Result> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, sem);
            if (roll != null) p.setString(2, roll);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    Result r = new Result();
                    r.roll = rs.getString(1); r.name = rs.getString(2); r.dept = rs.getString(3);
                    r.code = rs.getString(4); r.title = rs.getString(5); r.credits = rs.getInt(6); r.type = rs.getString(7);
                    int total = rs.getInt(8), att = rs.getInt(9);
                    r.attPct = total == 0 ? 0 : att * 100.0 / total;
                    r.condoned = rs.getBoolean(10);
                    r.cia1 = num(rs, 11); r.cia2 = num(rs, 12); r.asg = num(rs, 13); r.ese = num(rs, 14);
                    r.absent = rs.getBoolean(15);
                    evaluate(r);
                    out.add(r);
                }
            }
        } catch (SQLException e) { System.out.println("[ERROR] " + e.getMessage()); }
        return out;
    }

    private static Double num(ResultSet rs, int i) throws SQLException {
        double v = rs.getDouble(i);
        return rs.wasNull() ? null : v;
    }
}
