package com.ecodatahub.gus.service;

import com.ecodatahub.gus.domain.GUSDataAttribute;
import com.ecodatahub.gus.repository.GUSDataAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class GusDataAttributeService {

    private final GUSDataAttributeRepository gusDataAttributeRepository;

    public List<GUSDataAttribute> getGusDataAttributes() {
        return gusDataAttributeRepository.findAll();
    }

    public Optional<GUSDataAttribute> getGusDataAttribute(Integer id) {
        return gusDataAttributeRepository.findById(id);
    }

    public List<GUSDataAttribute> getAllAttributes() {
        return gusDataAttributeRepository.findAll();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public GUSDataAttribute createGusDataAttribute(GUSDataAttribute attribute) {
        return gusDataAttributeRepository.save(attribute);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<GUSDataAttribute> updateGusDataAttribute(Integer id, GUSDataAttribute attribute) {
        if (gusDataAttributeRepository.findById(id).isEmpty()) {
            return Optional.empty();
        }

        attribute.setId(id);

        return Optional.of(gusDataAttributeRepository.save(attribute));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean deleteGusDataAttribute(Integer id) {
        return gusDataAttributeRepository.findById(id)
                .map(attribute -> {
                    gusDataAttributeRepository.delete(attribute);
                    return true;
                })
                .orElse(false);
    }
}
