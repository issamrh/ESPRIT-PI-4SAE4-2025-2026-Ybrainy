import { GenerateApplicationRequest, GenerateApplicationResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

interface ApiErrorBody {
  message?: string;
  details?: string[];
}

export async function generateApplication(
  payload: GenerateApplicationRequest
): Promise<GenerateApplicationResponse> {
  const response = await fetch(`${API_BASE_URL}/api/generate-application`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as ApiErrorBody;
      if (body.message) {
        message = body.message;
      }
      if (body.details && body.details.length > 0) {
        message += `: ${body.details.join(', ')}`;
      }
    } catch {
      // Ignore JSON parsing errors and keep fallback message.
    }
    throw new Error(message);
  }

  return (await response.json()) as GenerateApplicationResponse;
}
