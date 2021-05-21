package com.employeepayrollservice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeePayrollService {

    public enum IOService {
        CONSOLE_IO, FILE_IO, DB_IO, REST_IO;
    }

    private List<EmployeePayrollData> employeePayrollList;
    private EmployeePayrollDBService employeePayrollDBService;

    public EmployeePayrollService(List<EmployeePayrollData> employeePayrollList) {
        this();
//        Create a new Memory so that size doesn't create trouble.
        this.employeePayrollList = new ArrayList<>(employeePayrollList);
    }

    public EmployeePayrollService() {
        employeePayrollDBService = EmployeePayrollDBService.getInstance();
    }

    public List<EmployeePayrollData> readEmployeePayrollData(IOService ioService) {
        if (ioService.equals(IOService.DB_IO))
            this.employeePayrollList = employeePayrollDBService.readData();
        return this.employeePayrollList;
    }

    public void addEmployeeToPayroll(List<EmployeePayrollData> employeePayrollList) {
        employeePayrollList.forEach(employeePayrollData -> {
            System.out.println("Employee being added : " + employeePayrollData.name);
            try {
                this.addEmployeeToPayrollDB(employeePayrollData.id, employeePayrollData.name,
                        employeePayrollData.gender, employeePayrollData.salary, employeePayrollData.startDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Employee added : " + employeePayrollData.name);
        });
        System.out.println("" + this.employeePayrollList);
    }

    public void addEmployeeToPayroll(EmployeePayrollData employeePayrollData, IOService ioService) {
        if (ioService.equals(IOService.REST_IO))
            employeePayrollList.add(employeePayrollData);
    }

    public void addEmployeeToPayrollWithThreads(List<EmployeePayrollData> asList) {
        Map<Integer, Boolean> employeeAdditionStatus = new HashMap<>();
        employeePayrollList.forEach(employeePayrollData -> {
            // creating task using runnable to execute the thread
            Runnable task = () -> {
                // employee payroll object id is set to false because get is not added
                employeeAdditionStatus.put(employeePayrollData.hashCode(), false);
                System.out.println("Employee being added : " + Thread.currentThread().getName());
                try {
                    this.addEmployeeToPayrollDB(employeePayrollData.id, employeePayrollData.name,
                            employeePayrollData.gender, employeePayrollData.salary, employeePayrollData.startDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                employeeAdditionStatus.put(employeePayrollData.hashCode(), true);
                System.out.println("Employee added : " + Thread.currentThread().getName());
            };
            // creating a thread and assigning to start the task
            Thread thread = new Thread(task, employeePayrollData.name);
            thread.start();
        });
        // keeping Main thread to wait
        while (employeeAdditionStatus.containsValue(false)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("" + this.employeePayrollList);
    }

    private void addEmployeeToPayrollDB(int emp_id, String name, String gender, double salary, LocalDate startDate) {
        employeePayrollList
                .add(employeePayrollDBService.addEmployeeToPayrollDB(emp_id, name, gender, salary, startDate));
    }

    public long countEntries(IOService ioService) {
        if (ioService.equals(IOService.DB_IO))
            return employeePayrollList.size();
        return employeePayrollList.size();
    }

    public void updateSalaryOfMultipleEmployees(Map<String, Double> employeeSalaryMap, IOService ioService) {
        Map<Integer, Boolean> salaryUpdateStatus = new HashMap<>();
        employeeSalaryMap.forEach((employee, salary) -> {
            Runnable salaryUpdate = () -> {
                salaryUpdateStatus.put(employee.hashCode(), false);
                System.out.println("Salary being updated : " + Thread.currentThread().getName());
                this.updateEmployeeSalary(employee, salary, ioService);
                salaryUpdateStatus.put(employee.hashCode(), true);
                System.out.println("Salary updated : " + Thread.currentThread().getName());
            };
            Thread thread = new Thread(salaryUpdate, employee);
            thread.start();
        });
        while (salaryUpdateStatus.containsValue(false)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("" + this.employeePayrollList);
    }


    public void updateEmployeeSalary(String name, double salary, IOService ioService) {
        if (ioService.equals(IOService.DB_IO)) {
            int result = employeePayrollDBService.updateEmployeeData(name, salary);
            if (result == 0)
                return;
        }
        EmployeePayrollData employeePayrollData = this.getEmployeeData(name);
        if (employeePayrollData != null)
            employeePayrollData.salary = salary;
    }

    public void deleteEmployeePayroll(String name, IOService ioService) {
        if (ioService.equals(IOService.REST_IO)){
            EmployeePayrollData employeePayrollData=this.getEmployeeData(name);
            employeePayrollList.remove(employeePayrollData);
        }
    }

    public EmployeePayrollData getEmployeeData(String name) {
        return this.employeePayrollList.stream()
                .filter(employeePayrollData -> employeePayrollData.name.equalsIgnoreCase(name)).findFirst()
                .orElse(null);
    }

    public boolean checkEmployeePayrollInSyncWithDB(String name) {
        List<EmployeePayrollData> employeeDataList = employeePayrollDBService.getEmployeeData(name);
        return employeeDataList.get(0).equals(this.getEmployeeData(name));
    }
}
