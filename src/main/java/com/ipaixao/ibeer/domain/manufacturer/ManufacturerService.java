package com.ipaixao.ibeer.domain.manufacturer;

import com.ipaixao.ibeer.domain.common.DuplicationValidator;
import com.ipaixao.ibeer.interfaces.incomming.manufacturer.dto.ManufacturerDTO;
import com.ipaixao.ibeer.interfaces.incomming.manufacturer.mapper.ManufacturerMapper;
import com.ipaixao.ibeer.interfaces.incomming.manufacturer.response.ManufacturerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
@Service
@RequiredArgsConstructor(access = PROTECTED)
public class ManufacturerService {

    private final ManufacturerRepository repository;
    private final ManufacturerMapper mapper;

    @Transactional
    public ManufacturerResponse create(ManufacturerDTO dto) {
        applyValidations(dto);
        return mapper.toResponse(repository.save(mapper.toEntity(dto)));
    }

    private void applyValidations(@NonNull ManufacturerDTO dto) {
        final var duplicatedManufacturer = Optional.of(dto)
                .filter(m -> nonNull(m.id()))
                .flatMap(m -> repository.findIdByNameAndIdNot(m.name(), m.id()))
                .orElseGet(() -> repository.findIdByName(dto.name()).orElse(null));

        new DuplicationValidator()
                .accept(duplicatedManufacturer);
    }

    @Transactional(readOnly = true)
    public Page<ManufacturerResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ManufacturerResponse> getAllActive(Pageable pageable) {
        return repository.findAllByActive(pageable, true).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse getById(long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    final var e = new EntityNotFoundException("Entity not found!");
                    log.error("m=getById, status=error, message={}", e.getMessage(), e);
                    return e;
                });
    }

    @Transactional
    public ManufacturerResponse update(ManufacturerDTO dto) {
        var manufacturerOptional = repository.findById(dto.id());

        manufacturerOptional
                .ifPresent(manufacturer -> mapAndSaveManufacturer(dto, manufacturer));

        if(manufacturerOptional.isEmpty())
            throw new EntityNotFoundException("Entity not found!");

        return mapper.toResponse(manufacturerOptional.get());
    }

    private void mapAndSaveManufacturer(ManufacturerDTO dto, Manufacturer manufacturer) {
        mapper.updateManufacturerFromDto(dto, manufacturer);
        repository.save(manufacturer);
    }

    @Transactional
    public void deleteById(long id) {
        repository.deleteById(id);
    }
}
