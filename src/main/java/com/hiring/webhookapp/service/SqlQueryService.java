package com.hiring.webhookapp.service;

import org.springframework.stereotype.Service;

@Service
public class SqlQueryService {

    public String getSqlQuery(String regNo) {
        // Extract last two digits
        String lastTwoDigits = regNo.replaceAll("\\D", "");
        int digits = Integer.parseInt(lastTwoDigits.substring(lastTwoDigits.length() - 2));

        if (digits % 2 == 1) {
            // Odd - Question 1
            return getQuestion1Query();
        } else {
            // Even - Question 2
            return getQuestion2Query();
        }
    }

    private String getQuestion1Query() {
        // Find the highest salaried employee per department, excluding payments on the 1st day of the month
        return "WITH FilteredPayments AS (" +
                "    SELECT " +
                "        p.EMP_ID, " +
                "        p.AMOUNT " +
                "    FROM PAYMENTS p " +
                "    WHERE DAY(p.PAYMENT_TIME) != 1" +
                "), " +
                "EmployeeSalaries AS (" +
                "    SELECT " +
                "        e.EMP_ID, " +
                "        e.FIRST_NAME, " +
                "        e.LAST_NAME, " +
                "        e.DOB, " +
                "        e.DEPARTMENT, " +
                "        SUM(fp.AMOUNT) AS SALARY " +
                "    FROM EMPLOYEE e " +
                "    INNER JOIN FilteredPayments fp ON e.EMP_ID = fp.EMP_ID " +
                "    GROUP BY e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, e.DOB, e.DEPARTMENT" +
                "), " +
                "RankedSalaries AS (" +
                "    SELECT " +
                "        es.*, " +
                "        ROW_NUMBER() OVER (PARTITION BY es.DEPARTMENT ORDER BY es.SALARY DESC) AS rn " +
                "    FROM EmployeeSalaries es" +
                ") " +
                "SELECT " +
                "    d.DEPARTMENT_NAME, " +
                "    rs.SALARY, " +
                "    CONCAT(rs.FIRST_NAME, ' ', rs.LAST_NAME) AS EMPLOYEE_NAME, " +
                "    TIMESTAMPDIFF(YEAR, rs.DOB, CURDATE()) AS AGE " +
                "FROM RankedSalaries rs " +
                "INNER JOIN DEPARTMENT d ON rs.DEPARTMENT = d.DEPARTMENT_ID " +
                "WHERE rs.rn = 1 " +
                "ORDER BY d.DEPARTMENT_ID";
    }

    private String getQuestion2Query() {
        // Calculate average age and concatenate names for employees earning > 70000 per department
        return "WITH HighEarners AS (" +
                "    SELECT DISTINCT " +
                "        e.EMP_ID, " +
                "        e.FIRST_NAME, " +
                "        e.LAST_NAME, " +
                "        e.DOB, " +
                "        e.DEPARTMENT, " +
                "        p.AMOUNT " +
                "    FROM EMPLOYEE e " +
                "    INNER JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
                "    WHERE p.AMOUNT > 70000" +
                "), " +
                "DepartmentStats AS (" +
                "    SELECT " +
                "        he.DEPARTMENT, " +
                "        AVG(TIMESTAMPDIFF(YEAR, he.DOB, CURDATE())) AS AVERAGE_AGE " +
                "    FROM HighEarners he " +
                "    GROUP BY he.DEPARTMENT" +
                "), " +
                "EmployeeNames AS (" +
                "    SELECT " +
                "        he.DEPARTMENT, " +
                "        CONCAT(he.FIRST_NAME, ' ', he.LAST_NAME) AS FULL_NAME, " +
                "        ROW_NUMBER() OVER (PARTITION BY he.DEPARTMENT ORDER BY he.EMP_ID) AS rn " +
                "    FROM HighEarners he" +
                ") " +
                "SELECT " +
                "    d.DEPARTMENT_NAME, " +
                "    ROUND(ds.AVERAGE_AGE, 2) AS AVERAGE_AGE, " +
                "    GROUP_CONCAT(en.FULL_NAME ORDER BY en.rn SEPARATOR ', ') AS EMPLOYEE_LIST " +
                "FROM DepartmentStats ds " +
                "INNER JOIN DEPARTMENT d ON ds.DEPARTMENT = d.DEPARTMENT_ID " +
                "LEFT JOIN EmployeeNames en ON ds.DEPARTMENT = en.DEPARTMENT AND en.rn <= 10 " +
                "GROUP BY d.DEPARTMENT_NAME, ds.AVERAGE_AGE, d.DEPARTMENT_ID " +
                "ORDER BY d.DEPARTMENT_ID DESC";
    }
}