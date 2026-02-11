package com.nakshedekho.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private InteriorProject project;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column
    private String fileType; // image, pdf, document, etc.

    @Column
    private Long fileSize; // in bytes

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String uploadedBy; // customer email or name

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
