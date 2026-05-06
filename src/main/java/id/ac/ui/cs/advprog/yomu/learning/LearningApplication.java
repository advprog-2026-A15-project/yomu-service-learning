package id.ac.ui.cs.advprog.yomu.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "id.ac.ui.cs.advprog.yomu.learning",
    "id.ac.ui.cs.advprog.yomu.shared" 
})
public class LearningApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningApplication.class, args);
    }
}