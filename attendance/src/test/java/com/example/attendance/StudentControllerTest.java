package com.example.attendance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerTest {
    @Autowired MockMvc mvc;
    @Autowired StudentRepository repo;

    @Test
    void addStudentAndMarkPresent() throws Exception {
        mvc.perform(post("/add").param("name", "Anish")).andExpect(status().is3xxRedirection());
        Student s = repo.findAll().get(0);
        assertEquals(false, s.isPresent());

        mvc.perform(post("/mark/" + s.getId()).param("present", "true"))
           .andExpect(status().is3xxRedirection());
        assertEquals(true, repo.findById(s.getId()).get().isPresent());
    }

    @Test
    void listPageLoads() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
