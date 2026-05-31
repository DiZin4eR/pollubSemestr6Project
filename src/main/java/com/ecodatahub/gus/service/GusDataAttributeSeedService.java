package com.ecodatahub.gus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ecodatahub.gus.domain.GUSDataAttribute;
import com.ecodatahub.gus.repository.GUSDataAttributeRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GusDataAttributeSeedService implements ApplicationRunner {

    private final GUSDataAttributeRepository repository;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void run(ApplicationArguments args) {
        repository.saveAll(List.of(
                attribute(0, "0", " ", ""),
                attribute(1, "wartość", " ", ""),
                attribute(3, "wartość", "k", "Agregat może być niekompletny"),
                attribute(4, "0", "x", "Brak informacji, konieczność zachowania tajemnicy statystycznej lub wypełnienie pozycji jest niemożliwe albo niecelowe"),
                attribute(7, "0", "a", "Wartość mniejsza niż przyjęty format prezentacji"),
                attribute(9, "wartość", "s", "Szacunki wstępne"),
                attribute(11, "wartość", "M", "Zmiany metodologiczne"),
                attribute(13, "wartość", "K", "Zmiany metodologiczne, agregat może być niekompletny"),
                attribute(14, "0", "X", "Zmiany metodologiczne, brak informacji, konieczność zachowania tajemnicy statystycznej lub wypełnienie pozycji jest niemożliwe albo niecelowe"),
                attribute(15, "-", " ", "Znak '-' oznacza brak informacji z powodu: zmiany poziomu prezentacji, zmian wprowadzonych do wykazu jednostek terytorialnych lub modyfikacji listy cech w danym okresie sprawozdawczym"),
                attribute(17, "0", "A", "Zmiany metodologiczne, wartość mniejsza niż przyjęty format prezentacji"),
                attribute(20, "wartość", "v", "Dane o niskiej precyzji"),
                attribute(21, "wartość", "v", "Dane o niskiej precyzji"),
                attribute(50, "- lub 0", "n", "Dana jeszcze niedostępna, będzie dostępna"),
                attribute(91, "0", "x", "Brak informacji, konieczność zachowania tajemnicy statystycznej lub wypełnienie pozycji jest niemożliwe albo niecelowe"),
                attribute(94, "0", "z", "Wartość znacząca, wartość zerowa wynika z bilansu niezerowych danych wejściowych algorytmu, np. przyrost naturalny, jeśli liczba zgonów jest równa liczbie urodzeń"),
                attribute(97, "wartość", "p", "Łącznie dla powiatu i miasta na prawach powiatu"),
                attribute(98, "0", "Z", "Zmiany metodologiczne, wartość znacząca, wartość zerowa wynika z bilansu niezerowych danych wejściowych algorytmu, np. przyrost naturalny, jeśli liczba zgonów jest równa liczbie urodzeń")
        ));
    }

    private GUSDataAttribute attribute(
            Integer id,
            String name,
            String symbol,
            String description
    ) {
        GUSDataAttribute attribute = new GUSDataAttribute();
        attribute.setId(id);
        attribute.setName(name);
        attribute.setSymbol(symbol);
        attribute.setDescription(description);

        return attribute;
    }
}
