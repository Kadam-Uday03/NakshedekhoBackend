package com.nakshedekho.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_stages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private InteriorProject project;
    
    @Column(nullable = false)
    private String stageName;
    
    @Column(nullable = false)
    private Boolean completed = false;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(length = 500)
    private String fileUrl;
    
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
