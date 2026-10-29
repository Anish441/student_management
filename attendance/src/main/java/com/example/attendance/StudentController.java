package com.example.attendance;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudentController {
    private final StudentRepository repo;
    public StudentController(StudentRepository repo) { this.repo = repo; }

    @GetMapping("/")
    public String list(Model model) {
        model.addAttribute("students", repo.findAll());
        return "index";
    }

    @PostMapping("/add")
    public String add(@RequestParam String name) {
        if (!name.isBlank()) repo.save(new Student(name.trim()));
        return "redirect:/";
    }

    @PostMapping("/mark/{id}")
    public String mark(@PathVariable Long id, @RequestParam boolean present) {
        repo.findById(id).ifPresent(s -> { s.setPresent(present); repo.save(s); });
        return "redirect:/";
    }
}
