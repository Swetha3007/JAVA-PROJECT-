import java.time.LocalDate;
import java.util.*;

/** Menu-driven console UI - KEC (Autonomous) Regulation 2024 Student Evaluation System. */
public class MainApp {
    private static final AcademicDAO dao = new AcademicDAO();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        if (!DBConnection.testConnection()) {
            System.out.println("Database connection failed. Check MySQL, kec_eval DB and DBConnection settings.");
            return;
        }
        boolean running = true;
        while (running) {
            System.out.println("\n=========== KEC STUDENT EVALUATION SYSTEM - R2024 ===========");
            System.out.println(" 1. Student Registration");
            System.out.println(" 2. Course Catalog");
            System.out.println(" 3. Course Enrollment & Attendance");
            System.out.println(" 4. Incremental Marks Entry (CIA1/2 | Assignment | ESE)");
            System.out.println(" 5. Student Semester Mark Sheet (SGPA)");
            System.out.println(" 6. Attendance Defaulter Report");
            System.out.println(" 7. Class Performance & Result Summary");
            System.out.println(" 8. Medical Condonation Approval");
            System.out.println(" 0. Exit");
            switch (ask("Enter your choice: ")) {
                case "1": registerStudent(); break;
                case "2": addCourse(); break;
                case "3": enrollStudent(); break;
                case "4": enterMarks(); break;
                case "5": markSheet(); break;
                case "6": defaulters(askInt("Semester: ")); break;
                case "7": departmentSummary(askInt("Semester: ")); break;
                case "8": condonation(); break;
                case "0": running = false; break;
                default: System.out.println("Invalid choice.");
            }
        }
        System.out.println("System closed.");
    }

    // ---------- input helpers ----------
    private static String ask(String msg) { System.out.print(msg); return sc.nextLine().trim(); }

    private static int askInt(String msg) {
        while (true) {
            try { return Integer.parseInt(ask(msg)); }
            catch (NumberFormatException e) { System.out.println("Enter a valid whole number."); }
        }
    }

    private static double askDouble(String msg) {
        while (true) {
            try { return Double.parseDouble(ask(msg)); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    // ---------- data entry ----------
    private static void registerStudent() {
        String roll = ask("Roll Number (e.g. 24ITR001): ").toUpperCase();
        String name = ask("Student Name: ");
        String dept = ask("Department (CSE/ECE/MECH/IT...): ");
        int sem = askInt("Current Semester (1-8): ");
        System.out.println(dao.addStudent(roll, name, dept, sem) ? "[SUCCESS] Student registered." : "[FAILED] Could not register.");
    }

    private static void addCourse() {
        String code = ask("Course Code (e.g. 24ITR31): ").toUpperCase();
        String title = ask("Course Title: ");
        int credits = askInt("Credits: ");
        String type = ask("Course Type (1.THEORY 2.PRACTICAL 3.EMBEDDED): ");
        type = type.equals("1") ? "THEORY" : type.equals("2") ? "PRACTICAL" : type.equals("3") ? "EMBEDDED" : "";
        if (type.isEmpty()) { System.out.println("Invalid course type."); return; }
        int sem = askInt("Offering Semester (1-8): ");
        System.out.println(dao.addCourse(code, title, credits, type, sem)
                ? "[SUCCESS] Course " + code + " [" + type + ", " + credits + " Credits] added." : "[FAILED] Could not add course.");
    }

    private static void enrollStudent() {
        String roll = ask("Roll Number: ").toUpperCase();
        String course = ask("Course Code: ").toUpperCase();
        int sem = askInt("Semester: ");
        int total = askInt("Total Classes Held: ");
        int attended = askInt("Classes Attended: ");
        if (attended > total || attended < 0) { System.out.println("Attended classes cannot exceed total classes."); return; }
        double pct = total == 0 ? 0 : attended * 100.0 / total;
        System.out.printf("Attendance: %.2f%%%n", pct);
        if (pct >= 75) System.out.println("[INFO] Eligible for ESE.");
        else if (pct >= 65) System.out.println("[INFO] Conditionally eligible - needs Medical Condonation (menu 8).");
        else System.out.println("[INFO] Shortage of Attendance - Grade SA, barred from ESE.");
        int id = dao.enroll(roll, course, sem, total, attended);
        System.out.println(id > 0 ? "[SUCCESS] Enrollment saved. (ID: " + id + ")" : "[FAILED] Check roll number / course code.");
    }

    private static void condonation() {
        int id = dao.getEnrollmentId(ask("Roll Number: ").toUpperCase(), ask("Course Code: ").toUpperCase(), askInt("Semester: "));
        if (id <= 0) { System.out.println("Enrollment not found."); return; }
        boolean ok = ask("Medical condonation approved? (y/n): ").equalsIgnoreCase("y");
        System.out.println(dao.setCondonation(id, ok) ? "[SUCCESS] Updated." : "[FAILED]");
    }

    private static void enterMarks() {
        String roll = ask("Roll Number: ").toUpperCase();
        String course = ask("Course Code: ").toUpperCase();
        int sem = askInt("Semester: ");
        int id = dao.getEnrollmentId(roll, course, sem);
        if (id <= 0) { System.out.println("Enrollment not found."); return; }
        System.out.println("1. Phase 1 (Mid-Sem): CIA Test 1 & 2");
        System.out.println("2. Phase 2 (End of Sem CIA): Assignment/Quiz/Lab");
        System.out.println("3. Phase 3 (Post Sem): ESE marks / Absent (AB)");
        boolean ok;
        switch (ask("Phase (1-3): ")) {
            case "1": ok = dao.recordPhase1Marks(id, askDouble("CIA Test 1 (0-100): "), askDouble("CIA Test 2 (0-100): ")); break;
            case "2": ok = dao.recordPhase2Marks(id, askDouble("Assignment/Quiz/Lab (0-100): ")); break;
            case "3":
                boolean ab = ask("Absent in ESE? (y/n): ").equalsIgnoreCase("y");
                ok = dao.recordPhase3Marks(id, ab ? null : askDouble("ESE marks (0-100): "), ab); break;
            default: System.out.println("Invalid phase."); return;
        }
        System.out.println(ok ? "[SUCCESS] Marks recorded." : "[FAILED] Marks not saved.");
    }

    // ---------- reports ----------
    private static void markSheet() {
        String roll = ask("Roll Number: ").toUpperCase();
        int sem = askInt("Semester: ");
        List<AcademicDAO.Result> list = dao.getResults(roll, sem);
        if (list.isEmpty()) { System.out.println("No enrollments found."); return; }
        String line = "=".repeat(128);
        System.out.println("\n" + line);
        System.out.println(center("KONGU ENGINEERING COLLEGE (AUTONOMOUS)", 128));
        System.out.println(center("GRADE SHEET - REGULATION 2024", 128));
        System.out.println(line);
        System.out.printf("Roll No: %s   Name: %s   Dept: %s   Semester: %d   Date: %s%n",
                roll, list.get(0).name, list.get(0).dept, sem, LocalDate.now());
        System.out.println("-".repeat(128));
        System.out.printf("%-9s %-28s %-9s %2s %6s %6s %6s %6s %7s %7s %7s %7s %-4s %-8s%n",
                "Course", "Title", "Type", "Cr", "Att%", "CIA1", "CIA2", "Asg", "Sc.CIA", "ESE", "Sc.ESE", "Total", "Gr", "Result");
        System.out.println("-".repeat(128));
        int earned = 0, enrolled = 0; int pts = 0;
        boolean arrears = false, pending = false;
        for (AcademicDAO.Result r : list) {
            enrolled += r.credits; pts += r.credits * r.gp;
            if (r.gp > 0) earned += r.credits;
            if (r.pending) pending = true; else if (r.gp == 0) arrears = true;
            System.out.printf("%-9s %-28s %-9s %2d %6.1f %6s %6s %6s %7s %7s %7s %7s %-4s %-8s%n",
                    r.code, cut(r.title, 28), r.type, r.credits, r.attPct, f(r.cia1), f(r.cia2), f(r.asg),
                    r.pending || r.gp == 0 && r.total == 0 ? "-" : String.format("%.2f", r.scaledCia),
                    r.absent ? "AB" : f(r.ese),
                    r.pending || r.gp == 0 && r.total == 0 ? "-" : String.format("%.2f", r.scaledEse),
                    r.pending || r.gp == 0 && r.total == 0 ? "-" : String.format("%.2f", r.total),
                    r.grade, r.status);
        }
        double sgpa = AcademicDAO.sgpa(list);
        System.out.println("-".repeat(128));
        System.out.printf("Total Credits: %d | Credits Earned: %d | Weighted Grade Points: %d%n", enrolled, earned, pts);
        System.out.printf("SGPA: %.2f / 10.00%s%n", sgpa, pending ? "  (provisional - some marks pending)" : "");
        System.out.printf("Equivalent Percentage: %.2f%%%n", sgpa * 10);
        System.out.println("Outcome: " + (pending ? "INCOMPLETE" : arrears ? "ARREARS - RE-APPEARANCE REQUIRED" : "PASSED - ALL COURSES CLEARED"));
        System.out.println(line);
        System.out.println("Grades: O(91-100)=10 | A+(81-90)=9 | A(71-80)=8 | B+(61-70)=7 | B(50-60)=6 | RA/SA/AB=0");
    }

    private static void defaulters(int sem) {
        List<AcademicDAO.Result> list = dao.getResults(null, sem);
        System.out.println("\nATTENDANCE DEFAULTER REPORT - SEMESTER " + sem);
        System.out.printf("%-10s %-18s %-6s %-9s %-7s %-14s%n", "Roll No", "Name", "Dept", "Course", "Att%", "Status");
        System.out.println("-".repeat(75));
        int n = 0;
        for (AcademicDAO.Result r : list) {
            if (r.attPct >= 75) continue;
            String st = r.attPct < 65 ? "SA (Barred)" : r.condoned ? "Condoned" : "SA (No condonation)";
            System.out.printf("%-10s %-18s %-6s %-9s %6.1f%% %-14s%n", r.roll, cut(r.name, 18), r.dept, r.code, r.attPct, st);
            n++;
        }
        if (n == 0) System.out.println("No defaulters found.");
        System.out.println("Note: <65% -> SA (barred) | 65-74.9% -> needs Medical Condonation");
    }

    private static void departmentSummary(int sem) {
        Map<String, List<AcademicDAO.Result>> byStudent = new LinkedHashMap<>();
        for (AcademicDAO.Result r : dao.getResults(null, sem)) byStudent.computeIfAbsent(r.roll, k -> new ArrayList<>()).add(r);

        class Stat { int total, passed, failed, incomplete; double sgpaSum; int counted; }
        Map<String, Stat> depts = new TreeMap<>();
        for (List<AcademicDAO.Result> courses : byStudent.values()) {
            Stat s = depts.computeIfAbsent(courses.get(0).dept, k -> new Stat());
            s.total++;
            boolean pend = courses.stream().anyMatch(r -> r.pending);
            boolean fail = courses.stream().anyMatch(r -> !r.pending && r.gp == 0);
            if (pend) { s.incomplete++; continue; }
            if (fail) s.failed++; else s.passed++;
            s.sgpaSum += AcademicDAO.sgpa(courses); s.counted++;
        }
        System.out.println("\nDEPARTMENT-WISE RESULT SUMMARY - SEMESTER " + sem);
        System.out.printf("%-10s %8s %8s %12s %11s %10s %9s%n", "Dept", "Students", "Passed", "With Arrear", "Incomplete", "Pass %", "Avg SGPA");
        System.out.println("-".repeat(75));
        for (Map.Entry<String, Stat> e : depts.entrySet()) {
            Stat s = e.getValue();
            double pr = s.counted == 0 ? 0 : s.passed * 100.0 / s.counted;
            double avg = s.counted == 0 ? 0 : s.sgpaSum / s.counted;
            System.out.printf("%-10s %8d %8d %12d %11d %9.2f%% %9.2f%n", e.getKey(), s.total, s.passed, s.failed, s.incomplete, pr, avg);
        }
    }

    private static String f(Double d) { return d == null ? "-" : String.format("%.1f", d); }
    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }
    private static String center(String s, int w) { return " ".repeat(Math.max(0, (w - s.length()) / 2)) + s; }
}
