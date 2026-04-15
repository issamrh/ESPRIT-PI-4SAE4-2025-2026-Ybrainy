package com.ybrainy.joboffer.service;

import com.ybrainy.joboffer.dto.ProfessionalPhotoOutcome;

public interface HuggingFaceService {

  ProfessionalPhotoOutcome enhanceProfilePhoto(byte[] imageBytes, String mimeType);
}
