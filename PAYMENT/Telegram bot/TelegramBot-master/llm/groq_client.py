"""
GROQ LLM Client - Handles communication with the GROQ API.

This module is responsible for:
1. Sending text to GROQ LLM
2. Receiving responses
3. Managing the connection
"""

from groq import Groq
from config.settings import Settings


class GroqClient:
    """Wrapper around Groq API for easy communication."""

    def __init__(self):
        """Initialize Groq client with API key."""
        self.client = Groq(api_key=Settings.GROQ_API_KEY)
        self.model = Settings.GROQ_MODEL

    def get_response(self, user_message: str) -> str:
        """
        Send a message to GROQ and get a response.

        Args:
            user_message: The text from the user

        Returns:
            str: The LLM response
        """
        try:
            # Send message to GROQ with system prompt
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": "You are a helpful assistant for an e-learning platform. You ONLY discuss topics related to online education, learning, courses, certifications, student resources, course management, and educational technology. If a user asks about something unrelated to e-learning, politely decline and redirect them back to e-learning topics. Be professional, helpful, and encouraging."
                    },
                    {
                        "role": "user",
                        "content": user_message,
                    }
                ],
                max_tokens=1024,  # Limit response length
                temperature=0.7,  # Balance between creative and focused
            )

            # Extract the text response
            return response.choices[0].message.content

        except Exception as e:
            # Return error message instead of crashing
            return f"❌ Error getting LLM response: {str(e)}"
