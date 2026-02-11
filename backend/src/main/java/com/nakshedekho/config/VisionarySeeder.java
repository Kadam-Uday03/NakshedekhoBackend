package com.nakshedekho.config;

import com.nakshedekho.model.Visionary;
import com.nakshedekho.repository.VisionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class VisionarySeeder implements CommandLineRunner {

        private final VisionaryRepository visionaryRepository;

        @Override
        public void run(String... args) throws Exception {
                if (visionaryRepository.count() == 0) {
                        List<Visionary> visionaries = Arrays.asList(
                                        new Visionary(null, "Ayudh Sanghvi", "Project Cordinator",
                                                        "8+ Years Experience",
                                                        "/images/ayudh.jpeg", true),
                                        new Visionary(null, "Ajay Patel", "Structural Engineer", "5+ Years Experience",
                                                        "/images/ajay patel.jpeg", true),
                                        new Visionary(null, "Nimisha Thakur", "Architect", "5+ Years Experience",
                                                        "/images/nimisha thakur.jpeg", true),
                                        new Visionary(null, "Ayan Sharma", "Project Manager", "8+ Years Experience",
                                                        "/images/ayan.jpeg",
                                                        true),
                                        new Visionary(null, "Ekta Mishra", "Civil Engineer", "8+ Years Experience",
                                                        "/images/manish.jpeg",
                                                        true),
                                        new Visionary(null, "Ayush Sarraf", "Structural Designer",
                                                        "10+ Years Experience",
                                                        "/images/ayush.jpeg", true),
                                        new Visionary(null, "Shubham Rajoriya", "Civil Engineer", "5+ Years Experience",
                                                        "/images/shubham.jpeg", true),
                                        new Visionary(null, "Mitali Sharma", "Architect", "3+ Years Experience",
                                                        "/images/mitali.jpeg",
                                                        true),
                                        new Visionary(null, "Taniya Mathe", "Architect", "3+ Years Experience",
                                                        "/images/taniya.jpeg",
                                                        true));
                        visionaryRepository.saveAll(visionaries);
                        System.out.println("Visionary team seeded successfully!");
                }
        }
}
