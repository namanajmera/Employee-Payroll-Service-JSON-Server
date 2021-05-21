package com.employeepayrollservice;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeePayrollDBService {
    private static EmployeePayrollDBService employeePayrollDBService;
    private int connectionCounter = 0;
    Connection connection = null;
    private PreparedStatement preparedStatement = null;

    // creating the object of Signature and getting instance
    public static EmployeePayrollDBService getInstance() {
        if (employeePayrollDBService == null) {
            employeePayrollDBService = new EmployeePayrollDBService();
        }
        return employeePayrollDBService;
    }

    public synchronized Connection getConnection() throws SQLException {
        connectionCounter++;
        String jdbcURL = "jdbc:mysql://localhost:3307/payroll_service";
        String userName = "root";
        String password = "Spider@6426";
        // the DriverManager class will attempt to load available JDBC drivers
        System.out.println("Processing Thread : " + Thread.currentThread().getName() + "Connecting to database : " + jdbcURL);
        connection = DriverManager.getConnection(jdbcURL, userName, password);
        // which thread is actually going to execute my connection
        System.out.println("Processing Thread : " + Thread.currentThread().getName() + " ID : " + connectionCounter
                + " Connection is successful! " + connection);
        return connection;
    }

    public List<EmployeePayrollData> readData() {
        String sql = "SELECT * FROM employee_payroll;";
        List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
        try (Connection connection = this.getConnection();) {
            // A SQL statement is precompiled and stored in a PreparedStatement object.
            // This object can then be used to efficiently execute this statement multiple
            // times.
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery(sql);
            employeePayrollList = this.getEmployeeData(resultSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return employeePayrollList;
    }

    public EmployeePayrollData addEmployeeToPayrollDB(int emp_id, String name, String gender, double salary,
                                                      LocalDate startDate) {
        int employeeId = -1;
        EmployeePayrollData employeePayrollData = null;
        String sql = String.format(
                "INSERT INTO employee_payroll (name,gender,salary,start) VALUES ('%s','%s','%s','%s')", name, gender,
                salary, Date.valueOf(startDate));
        try (Connection connection = this.getConnection();) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            // generated keys should be made available for retrieval
            int rowAffected = preparedStatement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            if (rowAffected == 1) {
                ResultSet resultSet = preparedStatement.getGeneratedKeys();
                if (resultSet.next())
                    employeeId = resultSet.getInt(1);
            }
            employeePayrollData = new EmployeePayrollData(employeeId, name, gender, salary, startDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Connection connection = this.getConnection();) {
            double deductions = salary * 0.2;
            double taxablePay = salary - deductions;
            double tax = taxablePay * 0.1;
            double netPay = salary - tax;
            String sql1 = String.format(
                    "INSERT INTO payroll_details(employee_id,basic_pay,deductions,taxable_pay,tax ,net_pay)VALUES (%s,%s,%s,%s,%s,%s)",
                    employeeId, salary, deductions, taxablePay, tax, netPay);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            int rowAffected = preparedStatement.executeUpdate(sql1);
            if (rowAffected == 1) {
                employeePayrollData = new EmployeePayrollData(employeeId, name, gender, salary, startDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return employeePayrollData;
    }

    public synchronized int updateEmployeeData(String name, Double salary) {
        return this.updateEmployeeDataUsingPreparedStatement(name, salary);
    }

    private int updateEmployeeDataUsingPreparedStatement(String name, Double salary) {
        String sql = String.format("UPDATE employee_payroll SET salary=%.2f WHERE name='%s'", salary, name);
        try (Connection connection = this.getConnection();) {
            PreparedStatement prepareStatement = connection.prepareStatement(sql);
            return prepareStatement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<EmployeePayrollData> getEmployeeData(String name) {
        List<EmployeePayrollData> employeePayrollList = null;
        if (this.preparedStatement == null)
            this.preparedStatementForEmployeeData();
        try {
            // Sets the designated parameter to the given Java String value
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            employeePayrollList = this.getEmployeeData(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employeePayrollList;
    }

    private List<EmployeePayrollData> getEmployeeData(ResultSet resultSet) throws SQLException {
        List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
        while (resultSet.next()) {
            int emp_id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            String gender = resultSet.getString("gender");
            double salary = resultSet.getDouble("salary");
            LocalDate startDate = resultSet.getDate("start").toLocalDate();
            employeePayrollList.add(new EmployeePayrollData(emp_id, name, gender, salary, startDate));
        }
        return employeePayrollList;
    }

    private void preparedStatementForEmployeeData() {
        try {
            Connection connection = this.getConnection();
            String sql = "SELECT * FROM employee_payroll WHERE name=?";
            preparedStatement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}