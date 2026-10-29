package com.example.attendance;
import jakarta.persistence.*;

@Entity
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean present;

    public Student() {}
    public Student(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
