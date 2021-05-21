package com.employeepayrollservice;

import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EmployeePayrollTest {
    @Test
    public void given6Employees_WhenAddedToDB_ShouldMatchEmployeeEntries() {
        EmployeePayrollData[] arrayOfEmps = {new EmployeePayrollData(1, "Jeff Bezos", "M", 100000.0, LocalDate.now()),
                new EmployeePayrollData(2, "Bill Gates", "M", 200000.0, LocalDate.now()),
                new EmployeePayrollData(3, "Mark Zuckerberg", "M", 300000.0, LocalDate.now()),
                new EmployeePayrollData(4, "Sunder Pichai", "M", 400000.0, LocalDate.now()),
                new EmployeePayrollData(5, "Mukesh", "M", 600000.0, LocalDate.now()),
                new EmployeePayrollData(6, "Anil", "M", 700000.0, LocalDate.now())};
        // creating object and reading data
        EmployeePayrollService employeePayrollService = new EmployeePayrollService();
        employeePayrollService.readEmployeePayrollData(EmployeePayrollService.IOService.DB_IO);
        // It records event time-stamps in the application
        Instant start = Instant.now();
        employeePayrollService.addEmployeeToPayroll(Arrays.asList(arrayOfEmps)); // adding employee to the payroll
        Instant end = Instant.now();
        System.out.println("Duration without thread : " + Duration.between(start, end));
        Instant threadStart = Instant.now();
        employeePayrollService.addEmployeeToPayrollWithThreads(Arrays.asList(arrayOfEmps));
        Instant threadEnd = Instant.now();
        System.out.println("Duration with Thread : " + Duration.between(threadStart, threadEnd));
        Assertions.assertEquals(20, employeePayrollService.countEntries(EmployeePayrollService.IOService.DB_IO));
    }

    @Test
    public void givenNewSalariesForMultipleEmployee_WhenUpdated_ShouldSyncWithDB() {
        EmployeePayrollService employeePayrollService = new EmployeePayrollService();
        employeePayrollService.readEmployeePayrollData(EmployeePayrollService.IOService.DB_IO);
        Map<String, Double> employeeSalaryMap = new HashMap<>();
        Instant threadStart = Instant.now();
        employeeSalaryMap.put("Anil", 3000000.00);
        employeeSalaryMap.put("Mukesh", 2000000.00);
        employeeSalaryMap.put("Sunder Pichai", 5000000.00);
        employeePayrollService.updateSalaryOfMultipleEmployees(employeeSalaryMap);
        Instant threadEnd = Instant.now();
        System.out.println("Duration with Thread : " + Duration.between(threadStart, threadEnd));
        boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Mukesh");
        Assertions.assertTrue(result);
    }

    //    Setting the URI and Post in Before so that we don't have to write again and again
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
    }

    private EmployeePayrollData[] getEmployeeList() {
        Response response = RestAssured.get("/employee_payroll");
        System.out.println("EMPLOYEE PAYROLL ENTRIES IN JSONServer:\n" + response.asString());
//        Converting the data from json format to array format so that we can read
        EmployeePayrollData[] arrayOfEmps = new Gson().fromJson(response.asString(), EmployeePayrollData[].class);
        return arrayOfEmps;
    }

    @Test
    public void givenEmployeeDataInJSONServer_WhenRetrieved_ShouldMatchTheCount(){
        EmployeePayrollData[] arrayOfEmps = getEmployeeList();
        EmployeePayrollService employeePayrollService = new EmployeePayrollService(Arrays.asList(arrayOfEmps));
        long entries=employeePayrollService.countEntries(EmployeePayrollService.IOService.REST_IO);
        Assertions.assertEquals(2,entries);
    }
}
