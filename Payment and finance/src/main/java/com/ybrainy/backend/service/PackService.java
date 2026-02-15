package com.ybrainy.backend.service;

import com.ybrainy.backend.dto.pack.CreatePackDTO;
import com.ybrainy.backend.dto.pack.PackResponseDTO;
import com.ybrainy.backend.dto.pack.UpdatePackDTO;
import com.ybrainy.backend.entity.Pack;
import com.ybrainy.backend.entity.PackCategory;
import com.ybrainy.backend.entity.enums.CategoryStatus;
import com.ybrainy.backend.entity.enums.PackLevel;
import com.ybrainy.backend.entity.enums.PackStatus;
import com.ybrainy.backend.exception.BusinessRuleException;
import com.ybrainy.backend.exception.ResourceNotFoundException;
import com.ybrainy.backend.mapper.PackMapper;
import com.ybrainy.backend.repository.PackCategoryRepository;
import com.ybrainy.backend.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PackService {

    private final PackRepository packRepository;
    private final PackCategoryRepository categoryRepository;
    private final PackMapper packMapper;

    /* ─── Admin: Create ─── */
    public PackResponseDTO create(CreatePackDTO dto) {
        // Business rule: salePrice ≤ originalPrice
        validatePrices(dto.getOriginalPrice(), dto.getSalePrice());

        PackCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        Pack entity = packMapper.toEntity(dto);
        entity.setCategory(category);
        entity.setStatus(PackStatus.DRAFT);

        Pack saved = packRepository.save(entity);
        return packMapper.toResponseDTO(saved);
    }

    /* ─── Admin: Update ─── */
    public PackResponseDTO update(Long id, UpdatePackDTO dto) {
        Pack entity = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));

        // Business rule: salePrice ≤ originalPrice
        validatePrices(dto.getOriginalPrice(), dto.getSalePrice());

        PackCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        packMapper.updateEntity(dto, entity);
        entity.setCategory(category);

        Pack updated = packRepository.save(entity);
        return packMapper.toResponseDTO(updated);
    }

    /* ─── Admin: Delete ─── */
    public void delete(Long id) {
        Pack entity = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));
        packRepository.delete(entity);
    }

    /* ─── Admin: Change Status ─── */
    public PackResponseDTO changeStatus(Long id, PackStatus newStatus) {
        Pack entity = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));

        // Business rule: If Category = INACTIVE → its Packs cannot be ACTIVE
        if (newStatus == PackStatus.ACTIVE && entity.getCategory().getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException("Cannot activate pack because its category '" + entity.getCategory().getName() + "' is INACTIVE");
        }

        entity.setStatus(newStatus);
        Pack updated = packRepository.save(entity);
        return packMapper.toResponseDTO(updated);
    }

    /* ─── Admin: Get By Id ─── */
    @Transactional(readOnly = true)
    public PackResponseDTO getById(Long id) {
        Pack entity = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));
        return packMapper.toResponseDTO(entity);
    }

    /* ─── Admin: Get All with Pagination & Filters ─── */
    @Transactional(readOnly = true)
    public Page<PackResponseDTO> getAllFiltered(Long categoryId, PackLevel level, PackStatus status, Pageable pageable) {
        return packRepository.findWithFilters(categoryId, level, status, pageable)
                .map(packMapper::toResponseDTO);
    }

    /* ─── Frontoffice: Get Active Packs ─── */
    @Transactional(readOnly = true)
    public List<PackResponseDTO> getActivePacks() {
        return packRepository.findByStatus(PackStatus.ACTIVE).stream()
                .map(packMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /* ─── Frontoffice: Get Active Packs by Category ─── */
    @Transactional(readOnly = true)
    public List<PackResponseDTO> getActivePacksByCategory(Long categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        return packRepository.findByCategoryIdAndStatus(categoryId, PackStatus.ACTIVE).stream()
                .map(packMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /* ─── Frontoffice: Get Single Pack (only if ACTIVE) ─── */
    @Transactional(readOnly = true)
    public PackResponseDTO getActivePackById(Long id) {
        Pack entity = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));
        if (entity.getStatus() != PackStatus.ACTIVE) {
            throw new ResourceNotFoundException("Pack not found with id: " + id);
        }
        return packMapper.toResponseDTO(entity);
    }

    /* ─── Price Validation ─── */
    private void validatePrices(Double originalPrice, Double salePrice) {
        if (salePrice > originalPrice) {
            throw new BusinessRuleException("Sale price (" + salePrice + ") cannot be greater than original price (" + originalPrice + ")");
        }
    }
}

