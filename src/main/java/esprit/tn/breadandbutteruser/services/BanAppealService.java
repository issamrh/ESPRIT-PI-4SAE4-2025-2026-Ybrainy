package esprit.tn.breadandbutteruser.services;

import esprit.tn.breadandbutteruser.dto.BanAppealRequestDto;
import esprit.tn.breadandbutteruser.dto.BanAppealResponseDto;
import esprit.tn.breadandbutteruser.entities.BanAppeal;
import esprit.tn.breadandbutteruser.entities.User;
import esprit.tn.breadandbutteruser.repositories.BanAppealRepository;
import esprit.tn.breadandbutteruser.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BanAppealService {

    private final BanAppealRepository banAppealRepository;
    private final UserRepository userRepository;

    public BanAppealResponseDto submit(BanAppealRequestDto request, User user) {
        BanAppeal banAppeal = BanAppeal.builder()
                .description(request.getDescription())
                .user(user)
                .build();

        banAppeal.submitAppeal();
        return toDto(banAppealRepository.save(banAppeal));
    }

    @Transactional(readOnly = true)
    public BanAppealResponseDto getById(Long id) {
        BanAppeal appeal = banAppealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ban appeal not found with ID: " + id));
        return toDto(appeal);
    }

    @Transactional(readOnly = true)
    public List<BanAppealResponseDto> getAll() {
        return banAppealRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BanAppealResponseDto> getByUserId(Long userId) {
        return banAppealRepository.findByUserUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BanAppealResponseDto> getByStatus(String status) {
        return banAppealRepository.findByAppealStatus(status).stream().map(this::toDto).collect(Collectors.toList());
    }

    public BanAppealResponseDto approve(Long id, String reviewedBy) {
        BanAppeal appeal = banAppealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ban appeal not found with ID: " + id));
        appeal.setReviewedBy(reviewedBy);
        appeal.approve();
        return toDto(banAppealRepository.save(appeal));
    }

    public BanAppealResponseDto reject(Long id, String reviewedBy) {
        BanAppeal appeal = banAppealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ban appeal not found with ID: " + id));
        appeal.setReviewedBy(reviewedBy);
        appeal.reject();
        return toDto(banAppealRepository.save(appeal));
    }

    public void delete(Long id) {
        if (!banAppealRepository.existsById(id)) {
            throw new RuntimeException("Ban appeal not found with ID: " + id);
        }
        banAppealRepository.deleteById(id);
    }

    private BanAppealResponseDto toDto(BanAppeal appeal) {
        return BanAppealResponseDto.builder()
                .appealId(appeal.getAppealId())
                .description(appeal.getDescription())
                .appealStatus(appeal.getAppealStatus())
                .submittedDate(appeal.getSubmittedDate())
                .resolvedDate(appeal.getResolvedDate())
                .reviewedBy(appeal.getReviewedBy())
                .userId(appeal.getUser() != null ? appeal.getUser().getUserId() : null)
                .build();
    }
}
