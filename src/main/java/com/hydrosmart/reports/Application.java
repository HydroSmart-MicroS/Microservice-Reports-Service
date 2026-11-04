package com.hydrosmart.reports;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class Application {
    public static void main(String[] args) { com.hydrosmart.reports.shared.infrastructure.configuration.RemoteConfiguration.load(); SpringApplication.run(Application.class, args); }
}
