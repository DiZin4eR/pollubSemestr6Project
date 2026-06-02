package com.ecodatahub.gus.service;

import com.ecodatahub.gus.domain.GUSDataAttribute;
import com.ecodatahub.gus.repository.GUSDataAttributeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GusDataAttributeServiceTest {

    @Mock
    private GUSDataAttributeRepository repository;

    @InjectMocks
    private GusDataAttributeService service;

    @Test
    void updateGusDataAttributeSavesWithRequestedIdWhenEntityExists() {
        GUSDataAttribute existing = attribute(1);
        GUSDataAttribute update = attribute(null);
        when(repository.findById(1)).thenReturn(Optional.of(existing));
        when(repository.save(update)).thenReturn(update);

        Optional<GUSDataAttribute> result = service.updateGusDataAttribute(1, update);

        assertThat(result).containsSame(update);
        assertThat(update.getId()).isEqualTo(1);
        verify(repository).save(update);
    }

    @Test
    void updateGusDataAttributeReturnsEmptyWhenEntityDoesNotExist() {
        GUSDataAttribute update = attribute(null);
        when(repository.findById(99)).thenReturn(Optional.empty());

        Optional<GUSDataAttribute> result = service.updateGusDataAttribute(99, update);

        assertThat(result).isEmpty();
        verify(repository, never()).save(update);
    }

    @Test
    void deleteGusDataAttributeDeletesAndReportsTrueWhenEntityExists() {
        GUSDataAttribute existing = attribute(1);
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        boolean deleted = service.deleteGusDataAttribute(1);

        assertThat(deleted).isTrue();
        verify(repository).delete(existing);
    }

    @Test
    void deleteGusDataAttributeReportsFalseWhenEntityDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        boolean deleted = service.deleteGusDataAttribute(99);

        assertThat(deleted).isFalse();
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private GUSDataAttribute attribute(Integer id) {
        GUSDataAttribute attribute = new GUSDataAttribute();
        attribute.setId(id);
        attribute.setName("name");
        attribute.setSymbol("symbol");
        attribute.setDescription("description");
        return attribute;
    }
}
