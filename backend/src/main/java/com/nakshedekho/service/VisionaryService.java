package com.nakshedekho.service;

import com.nakshedekho.model.Visionary;
import com.nakshedekho.repository.VisionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisionaryService {

    private final VisionaryRepository visionaryRepository;

    public List<Visionary> getAllVisionaries() {
        return visionaryRepository.findAll();
    }

    public List<Visionary> getActiveVisionaries() {
        return visionaryRepository.findByActiveTrue();
    }

    public Visionary createVisionary(Visionary visionary) {
        return visionaryRepository.save(visionary);
    }

    public Visionary updateVisionary(Long id, Visionary visionaryDetails) {
        Visionary visionary = visionaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visionary not found"));

        visionary.setName(visionaryDetails.getName());
        visionary.setRole(visionaryDetails.getRole());
        visionary.setExperience(visionaryDetails.getExperience());
        visionary.setImageUrl(visionaryDetails.getImageUrl());
        visionary.setActive(visionaryDetails.getActive());

        return visionaryRepository.save(visionary);
    }

    public void deleteVisionary(Long id) {
        visionaryRepository.deleteById(id);
    }
}
