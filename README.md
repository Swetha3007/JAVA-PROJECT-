# KEC R2024 Student Academic & Continuous Evaluation System (Java JDBC + MySQL)

Console application to manage students, courses, enrollments, attendance, incremental marks entry
(CIA1/CIA2 -> Assignment -> ESE) and generate KEC Regulation 2024 grade sheets.

## Files
| File | Purpose |
|---|---|
| `kec_schema.sql` | Tables with constraints & foreign keys |
| `sample_data.sql` | Optional sample students/courses |
| `DBConnection.java` | JDBC connection management |
| `AcademicDAO.java` | SQL CRUD + evaluation engine (weightage, grading, SGPA) |
| `MainApp.java` | Menu-driven interface and reports |

## Rules implemented
- THEORY 40 CIA / 60 ESE (ESE min 45%) | PRACTICAL 60/40 (min 50%) | EMBEDDED 50/50 (min 45%) | pass total >= 50
- Attendance: >=75% eligible, 65-74.9% needs condonation, <65% = SA
- Grades: O 10, A+ 9, A 8, B+ 7, B 6, RA/SA/AB 0
- SGPA = sum(credits x GP) / sum(enrolled credits); Percentage = SGPA x 10
- CIA component (out of 100) = average of CIA1, CIA2 and Assignment, then scaled by weightage

## Run
```bash
mysql -u root -p < kec_schema.sql
mysql -u root -p < sample_data.sql      # optional
javac -cp ".;mysql-connector-j-8.x.jar" *.java          # Linux/Mac: use ':' instead of ';'
java  -cp ".;mysql-connector-j-8.x.jar" MainApp
```
DB settings default to `localhost:3306/kec_eval`, user `root`, password `root`.
Override with env vars `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
