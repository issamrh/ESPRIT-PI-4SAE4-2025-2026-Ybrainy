package com.ybrainy.joboffer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ybrainy.joboffer.dto.ProfessionalPhotoOutcome;
import com.ybrainy.joboffer.exception.ExternalServiceException;
import com.ybrainy.joboffer.service.GeminiService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeminiServiceImpl implements GeminiService {

  private static final String DEFAULT_CV_STRUCTURE =
      """
      Use a standard professional layout with clear French section headings in this order:
      [CONTACT] [PROFIL PROFESSIONNEL] [EXPERIENCE] [FORMATION] [COMPETENCES] [LANGUES] (omit LANGUES if unknown).
      Plain text only, ATS-friendly.
      """
          .trim();

  private static final String COVER_LETTER_PROMPT =
      """
      Write a professional cover letter based on this CV and job description.
      Highlight relevant skills.
      Do not invent information.
      Keep a clear and persuasive tone.

      Return only the cover letter in plain text.

      CV:
      %s

      Job description:
      %s
      """;

  private static final String PROFILE_IMAGE_GEN_PROMPT =
      """
      IMAGE EDITING TASK — use the attached photograph as the ONLY source.

      Produce exactly ONE output image: a professionally retouched portrait of the SAME person
      (keep the same face, age, and recognizable identity — do not replace with a different person).

      Apply these modifications directly to this image:
      - Replace the background with a clean neutral studio backdrop (soft light gray gradient, no beach/outdoor clutter).
      - Re-crop or re-frame to a CV/LinkedIn head-and-shoulders or bust portrait; remove distracting props (e.g. fishing rod, fish) from the composition.
      - Use soft, even, flattering studio-style lighting on the face (no harsh shadows).
      - If clothing is clearly too casual for business, adjust to a professional look (shirt, blouse, or blazer) while staying consistent with the same individual.

      You MUST return the result as an image in your response (not only text). Optional: one short French sentence describing what you changed.
      """
          .trim();

  private static final String PROFILE_PHOTO_TIPS_PROMPT =
      """
      Tu es expert en photo de profil professionnelle. L'utilisateur envoie sa photo actuelle pour un CV.
      Analyse l'image et reponds UNIQUEMENT en francais avec :
      1) Un court paragraphe (2-3 phrases) sur l'impression actuelle de la photo.
      2) Une liste numerotee de 5 conseils concrets (lumiere, fond, cadrage, tenue, expression) pour la rendre plus professionnelle.
      Ne pas inventer d'informations sur la personne. Si le visage n'est pas visible, dis-le et donne des conseils generaux.
      """
          .trim();

  private final RestTemplate restTemplate;
  private final String geminiApiKey;
  private final String geminiApiUrl;
  private final String geminiApiModel;
  private final List<String> geminiImageModels;

  public GeminiServiceImpl(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${gemini.api.key:}") String geminiApiKey,
      @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}") String geminiApiUrl,
      @Value("${gemini.api.model:gemini-2.5-flash}") String geminiApiModel,
      @Value(
              "${gemini.api.image-models:gemini-2.5-flash-image,gemini-3.1-flash-image-preview,gemini-2.0-flash-preview-image-generation}")
          String geminiImageModelsRaw) {
    this.restTemplate = restTemplateBuilder.build();
    this.geminiApiKey = geminiApiKey;
    this.geminiApiUrl = geminiApiUrl;
    this.geminiApiModel = geminiApiModel;
    this.geminiImageModels = parseImageModels(geminiImageModelsRaw);
  }

  private static List<String> parseImageModels(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of(
          "gemini-2.5-flash-image",
          "gemini-3.1-flash-image-preview",
          "gemini-2.0-flash-preview-image-generation");
    }
    return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  @Override
  public String generateOptimizedCV(String cv, String jobDescription, String cvSkeleton) {
    String skeletonBlock =
        cvSkeleton == null || cvSkeleton.isBlank() ? DEFAULT_CV_STRUCTURE : cvSkeleton.trim();
    String prompt = buildOptimizedCvPrompt(skeletonBlock, cv, jobDescription);
    return generateFromPrompt(prompt);
  }

  private static String buildOptimizedCvPrompt(String skeletonBlock, String cv, String jobDescription) {
    return """
        Rewrite this CV to match the job description.
        Highlight relevant skills.
        Do not invent information.
        Make it ATS-friendly.

        Return only the optimized CV in professional plain text.

        CV structure requirement:
        """
        + skeletonBlock
        + """

        CV:
        """
        + cv
        + """

        Job description:
        """
        + jobDescription;
  }

  @Override
  public String generateCoverLetter(String cv, String jobDescription) {
    String prompt = COVER_LETTER_PROMPT.formatted(cv, jobDescription);
    return generateFromPrompt(prompt);
  }

  @Override
  public ProfessionalPhotoOutcome enhanceProfilePhoto(byte[] imageBytes, String mimeType) {
    if (geminiApiKey == null || geminiApiKey.isBlank()) {
      return new ProfessionalPhotoOutcome(
          null, "GEMINI_API_KEY manquante : impossible de traiter la photo.");
    }
    String mime = normalizeImageMime(mimeType);
    String b64 = Base64.getEncoder().encodeToString(imageBytes);

    Optional<ProfessionalPhotoOutcome> generated = tryGenerateProfessionalPortrait(b64, mime);
    if (generated.isPresent()) {
      return generated.get();
    }

    try {
      String tips = generateProfilePhotoTipsMultimodal(b64, mime);
      if (tips != null && !tips.isBlank()) {
        return new ProfessionalPhotoOutcome(
            null,
            "Aucun modele image n a renvoye de photo editee (verifie gemini.api.image-models et les droits de ta cle API). "
                + "Conseils pour refaire la photo toi-meme :\n\n"
                + tips);
      }
    } catch (Exception ignored) {
      // fall through
    }
    return new ProfessionalPhotoOutcome(
        null,
        "Impossible de generer une image professionnelle. Verifie gemini.api.image-models (modeles image Google AI) et ta cle API.");
  }

  private Map<String, Object> buildProfessionalPhotoPayload(String base64Data, String mime) {
    Map<String, Object> inline = new LinkedHashMap<>();
    inline.put("mime_type", mime);
    inline.put("data", base64Data);

    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("inline_data", inline));
    parts.add(Map.of("text", PROFILE_IMAGE_GEN_PROMPT));

    Map<String, Object> userTurn = new LinkedHashMap<>();
    userTurn.put("role", "user");
    userTurn.put("parts", parts);

    Map<String, Object> imageConfig = new LinkedHashMap<>();
    imageConfig.put("aspectRatio", "3:4");
    imageConfig.put("imageSize", "1K");

    Map<String, Object> genConfig = new LinkedHashMap<>();
    genConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
    genConfig.put("imageConfig", imageConfig);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("contents", List.of(userTurn));
    payload.put("generationConfig", genConfig);
    return payload;
  }

  /** Payload sans imageConfig (compatibilite modeles plus anciens). */
  private Map<String, Object> buildProfessionalPhotoPayloadSimple(String base64Data, String mime) {
    Map<String, Object> inline = new LinkedHashMap<>();
    inline.put("mime_type", mime);
    inline.put("data", base64Data);
    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("inline_data", inline));
    parts.add(Map.of("text", PROFILE_IMAGE_GEN_PROMPT));
    Map<String, Object> userTurn = new LinkedHashMap<>();
    userTurn.put("role", "user");
    userTurn.put("parts", parts);
    Map<String, Object> genConfig = new LinkedHashMap<>();
    genConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("contents", List.of(userTurn));
    payload.put("generationConfig", genConfig);
    return payload;
  }

  private Optional<ProfessionalPhotoOutcome> tryGenerateProfessionalPortrait(String base64Data, String mime) {
    if (geminiImageModels == null || geminiImageModels.isEmpty()) {
      return Optional.empty();
    }
    List<Map<String, Object>> payloads =
        List.of(buildProfessionalPhotoPayload(base64Data, mime), buildProfessionalPhotoPayloadSimple(base64Data, mime));

    for (Map<String, Object> payload : payloads) {
      for (String model : geminiImageModels) {
        if (model == null || model.isBlank()) {
          continue;
        }
        try {
          JsonNode body = postForModel(model.trim(), payload);
          if (body == null) {
            continue;
          }
          ParsedParts parsed = parseContentParts(body);
          if (parsed.imageBase64 != null && !parsed.imageBase64.isBlank()) {
            String outMime = parsed.imageMime != null ? parsed.imageMime : "image/png";
            String dataUrl = "data:" + outMime + ";base64," + parsed.imageBase64;
            String note = parsed.text != null && !parsed.text.isBlank() ? parsed.text : null;
            return Optional.of(new ProfessionalPhotoOutcome(dataUrl, note));
          }
        } catch (HttpClientErrorException ex) {
          int code = ex.getStatusCode().value();
          if (code == 404 || code == 400) {
            continue;
          }
          break;
        } catch (Exception ignored) {
          // try next model / payload variant
        }
      }
    }
    return Optional.empty();
  }

  private String generateProfilePhotoTipsMultimodal(String base64Data, String mime) {
    Map<String, Object> inline = new LinkedHashMap<>();
    inline.put("mime_type", mime);
    inline.put("data", base64Data);

    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("inline_data", inline));
    parts.add(Map.of("text", PROFILE_PHOTO_TIPS_PROMPT));

    Map<String, Object> payload =
        new LinkedHashMap<>(Map.of("contents", List.of(Map.of("parts", parts))));

    JsonNode body = postForModel(geminiApiModel.trim(), payload);
    if (body == null) {
      return "";
    }
    return parseContentParts(body).text;
  }

  private JsonNode postForModel(String modelName, Map<String, Object> payload) {
    if (geminiApiKey == null || geminiApiKey.isBlank()) {
      throw new ExternalServiceException("GEMINI_API_KEY is missing.");
    }
    String safeApiKey = geminiApiKey.trim();
    String endpoint = buildGenerateContentUrl(modelName, safeApiKey);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-goog-api-key", safeApiKey);
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
    ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpoint, request, JsonNode.class);
    return response.getBody();
  }

  private String generateFromPrompt(String prompt) {
    if (geminiApiKey == null || geminiApiKey.isBlank()) {
      throw new ExternalServiceException("GEMINI_API_KEY is missing. Configure it before calling this endpoint.");
    }

    Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

    String safeApiKey = geminiApiKey.trim();
    String endpoint = buildGenerateContentUrl(geminiApiModel.trim(), safeApiKey);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-goog-api-key", safeApiKey);
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

    try {
      ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpoint, request, JsonNode.class);
      JsonNode body = response.getBody();
      if (body == null) {
        throw new ExternalServiceException("Gemini returned an empty response.");
      }

      String text = extractAllTextFromResponse(body);
      if (text.isBlank()) {
        throw new ExternalServiceException("Gemini returned an empty text response.");
      }
      return text;
    } catch (HttpClientErrorException.TooManyRequests ex) {
      throw new ExternalServiceException("Gemini API rate limit reached (429). Please retry in a moment.", ex);
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == 404) {
        throw new ExternalServiceException(
            "Gemini model endpoint not found (404). Check gemini.api.model=\""
                + geminiApiModel
                + "\" and gemini.api.url configuration.",
            ex);
      }
      if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
        throw new ExternalServiceException("Gemini API key is invalid or unauthorized.", ex);
      }
      throw new ExternalServiceException(
          "Gemini API error (" + ex.getStatusCode().value() + " " + ex.getStatusText() + ").", ex);
    } catch (RestClientException ex) {
      String detail = rootCauseMessage(ex);
      throw new ExternalServiceException(
          "Unable to reach Gemini API. Check network, proxy/firewall, SSL, and API URL."
              + (detail.isBlank() ? "" : " Cause: " + detail),
          ex);
    }
  }

  private static String extractAllTextFromResponse(JsonNode response) {
    ParsedParts p = parseContentParts(response);
    return p.text;
  }

  private static ParsedParts parseContentParts(JsonNode body) {
    StringBuilder text = new StringBuilder();
    String imageB64 = null;
    String imageMime = null;
    JsonNode candidates = body.path("candidates");
    if (!candidates.isArray()) {
      return new ParsedParts("", null, null);
    }
    for (JsonNode cand : candidates) {
      JsonNode parts = cand.path("content").path("parts");
      if (!parts.isArray()) {
        continue;
      }
      for (JsonNode part : parts) {
        if (part.has("text")) {
          text.append(part.get("text").asText(""));
        }
        JsonNode inline = part.get("inlineData");
        if (inline == null || inline.isMissingNode()) {
          inline = part.get("inline_data");
        }
        if (inline != null && !inline.isMissingNode()) {
          JsonNode dataNode = inline.get("data");
          if (dataNode != null && dataNode.isTextual()) {
            String d = dataNode.asText("");
            if (imageB64 == null && d != null && !d.isBlank()) {
              imageB64 = d;
            }
          }
          JsonNode mt = inline.get("mimeType");
          if (mt == null || mt.isMissingNode()) {
            mt = inline.get("mime_type");
          }
          if (mt != null && mt.isTextual() && imageMime == null) {
            imageMime = mt.asText("");
          }
        }
      }
    }
    return new ParsedParts(text.toString().trim(), imageMime, imageB64);
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

  private String buildGenerateContentUrl(String model, String apiKey) {
    String base = geminiApiUrl == null ? "" : geminiApiUrl.trim();
    String m = model == null ? "" : model.trim();
    if (base.isBlank() || m.isBlank()) {
      throw new ExternalServiceException("Gemini API url/model is missing. Check gemini.api.url and model name.");
    }
    String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    String full = normalizedBase + "/" + m + ":generateContent";
    return UriComponentsBuilder.fromHttpUrl(full).queryParam("key", apiKey).toUriString();
  }

  private String rootCauseMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    String message = current.getMessage();
    return message == null ? "" : message.trim();
  }

  private record ParsedParts(String text, String imageMime, String imageBase64) {}
}
