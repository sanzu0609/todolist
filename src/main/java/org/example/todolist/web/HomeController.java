package org.example.todolist.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the landing page and simple health probe until dedicated monitoring is added.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Render the placeholder home page; Thymeleaf template will be supplied in T0.5.
        return "home/index";
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
