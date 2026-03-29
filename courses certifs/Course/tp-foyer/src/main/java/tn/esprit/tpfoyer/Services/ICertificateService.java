package tn.esprit.tpfoyer.Services;

import org.springframework.http.ResponseEntity;
import tn.esprit.tpfoyer.Dto.VerificationResponseDTO;

public interface ICertificateService {
    ResponseEntity<?> generateCertificate(Long courseId, Long studentId);
    VerificationResponseDTO verifyCertificate(String certificateId);
}
