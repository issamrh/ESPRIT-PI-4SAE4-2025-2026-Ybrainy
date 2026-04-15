package com.ybrainy.joboffer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ybrainy.joboffer.dto.ProfessionalPhotoOutcome;
import com.ybrainy.joboffer.exception.ExternalServiceException;
import com.ybrainy.joboffer.service.HuggingFaceService;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class HuggingFaceServiceImpl implements HuggingFaceService {

  private static final String IMAGE_EDIT_PROMPT =
      """
      Transform this photo into a clean, professional CV/LinkedIn headshot.
      Keep the same person and face identity.
      Use neutral studio background, balanced lighting, and business-friendly appearance.
      Remove visual distractions and keep a realistic photographic style.
      """
          .trim();

  private static final String NEGATIVE_PROMPT =
      "blurry, distorted face, extra limbs, cartoon, low quality, watermark, text, logo";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String apiToken;
  private final String apiUrl;
  private final String imageModel;

  public HuggingFaceServiceImpl(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper,
      @Value("${huggingface.api.token:}") String apiToken,
      @Value("${huggingface.api.url:https://api-inference.huggingface.co/models}") String apiUrl,
      @Value("${huggingface.api.image-model:timbrooks/instruct-pix2pix}") String imageModel) {
    this.restTemplate = restTemplateBuilder.build();
    this.objectMapper = objectMapper;
    this.apiToken = apiToken;
    this.apiUrl = apiUrl;
    this.imageModel = imageModel;
  }

  @Override
  public ProfessionalPhotoOutcome enhanceProfilePhoto(byte[] imageBytes, String mimeType) {
    if (apiToken == null || apiToken.isBlank()) {
      return new ProfessionalPhotoOutcome(
          null, "HUGGINGFACE_API_TOKEN manquante : impossible de traiter la photo.");
    }

    String safeMime = normalizeImageMime(mimeType);
    String b64 = Base64.getEncoder().encodeToString(imageBytes);
    String endpoint = buildEndpoint();

    try {
      // 1) Try image-to-image request (preferred)
      Map<String, Object> imageInput = new LinkedHashMap<>();
      imageInput.put("image", b64);
      imageInput.put("prompt", IMAGE_EDIT_PROMPT);

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("inputs", imageInput);
      payload.put("parameters", Map.of("negative_prompt", NEGATIVE_PROMPT));
      payload.put("options", Map.of("wait_for_model", true));

      ProfessionalPhotoOutcome out = callForImage(endpoint, payload, safeMime);
      if (out.professionalProfilePhotoDataUrl() != null) {
        return out;
      }

      // 2) Fallback to text-to-image if the selected model does not accept image-to-image schema.
      Map<String, Object> fallbackPayload = new LinkedHashMap<>();
      fallbackPayload.put("inputs", IMAGE_EDIT_PROMPT);
      fallbackPayload.put("parameters", Map.of("negative_prompt", NEGATIVE_PROMPT));
      fallbackPayload.put("options", Map.of("wait_for_model", true));

      ProfessionalPhotoOutcome fallback = callForImage(endpoint, fallbackPayload, "image/png");
      if (fallback.professionalProfilePhotoDataUrl() != null) {
        return new ProfessionalPhotoOutcome(
            fallback.professionalProfilePhotoDataUrl(),
            "Mode image-to-image indisponible sur ce modele. Photo generee via fallback text-to-image.");
      }
    } catch (ExternalServiceException ex) {
      return new ProfessionalPhotoOutcome(null, ex.getMessage());
    } catch (Exception ex) {
      return new ProfessionalPhotoOutcome(
          null, "Generation photo via Hugging Face impossible pour le moment.");
    }

    return new ProfessionalPhotoOutcome(
        null,
        "Aucun resultat image renvoye par Hugging Face. Verifie huggingface.api.image-model et les droits du token.");
  }

  private ProfessionalPhotoOutcome callForImage(String endpoint, Map<String, Object> payload, String defaultMime) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiToken.trim());

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

    try {
      ResponseEntity<byte[]> response =
          restTemplate.exchange(endpoint, HttpMethod.POST, request, byte[].class);

      byte[] body = response.getBody();
      if (body == null || body.length == 0) {
        return new ProfessionalPhotoOutcome(null, null);
      }

      MediaType contentType = response.getHeaders().getContentType();
      if (contentType != null && contentType.getType().equalsIgnoreCase("image")) {
        String dataUrl =
            "data:" + contentType.toString() + ";base64," + Base64.getEncoder().encodeToString(body);
        return new ProfessionalPhotoOutcome(dataUrl, null);
      }

      JsonNode json = objectMapper.readTree(body);
      if (json.has("error")) {
        String error = json.path("error").asText("");
        if (!error.isBlank()) {
          throw new ExternalServiceException("Hugging Face error: " + error);
        }
      }

      String b64 = extractImageBase64FromJson(json);
      if (b64 == null || b64.isBlank()) {
        return new ProfessionalPhotoOutcome(null, null);
      }
      String dataUrl = "data:" + defaultMime + ";base64," + b64;
      return new ProfessionalPhotoOutcome(dataUrl, null);
    } catch (HttpClientErrorException ex) {
      int status = ex.getStatusCode().value();
      if (status == 401 || status == 403) {
        throw new ExternalServiceException("Hugging Face token invalide ou non autorise.", ex);
      }
      throw new ExternalServiceException(
          "Hugging Face API error (" + status + " " + ex.getStatusText() + ").", ex);
    } catch (RestClientException ex) {
      throw new ExternalServiceException(
          "Impossible de joindre Hugging Face. Verifie le reseau et huggingface.api.url.", ex);
    } catch (Exception ex) {
      throw new ExternalServiceException("Reponse Hugging Face invalide pour la generation image.", ex);
    }
  }

  private static String extractImageBase64FromJson(JsonNode json) {
    if (json == null || json.isMissingNode()) {
      return null;
    }

    if (json.has("image") && json.path("image").isTextual()) {
      return json.path("image").asText("");
    }
    if (json.has("generated_image") && json.path("generated_image").isTextual()) {
      return json.path("generated_image").asText("");
    }

    if (json.isArray() && json.size() > 0) {
      JsonNode first = json.get(0);
      if (first.has("image") && first.path("image").isTextual()) {
        return first.path("image").asText("");
      }
      if (first.has("generated_image") && first.path("generated_image").isTextual()) {
        return first.path("generated_image").asText("");
      }
    }

    return null;
  }

  private static String normalizeImageMime(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      return "image/jpeg";
    }
    String m = mimeType.trim().toLowerCase();
    if ("image/jpg".equals(m)) {
      return "image/jpeg";
    }
    if ("image/jpeg".equals(m) || "image/png".equals(m) || "image/webp".equals(m)) {
      return m;
    }
    return "image/jpeg";
  }

  private String buildEndpoint() {
    if (apiUrl == null || apiUrl.isBlank() || imageModel == null || imageModel.isBlank()) {
      throw new ExternalServiceException(
          "Configuration Hugging Face incomplete. Verifie huggingface.api.url et huggingface.api.image-model.");
    }
    String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    return base + "/" + imageModel.trim();
  }
}
