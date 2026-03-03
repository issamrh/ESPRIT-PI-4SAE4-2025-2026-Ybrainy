"""
Configuration module - Loads environment variables safely.

This module handles all configuration from the .env file.
It's the single place where we access credentials.
"""

import os
from dotenv import load_dotenv

# Load variables from .env file
load_dotenv()


class Settings:
    """Stores all configuration from environment variables."""

    # Telegram Bot Token - Get from BotFather on Telegram
    TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")

    # GROQ API Key - Get from console.groq.com
    GROQ_API_KEY = os.getenv("GROQ_API_KEY")

    # GROQ Model to use
    GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")

    # MySQL Database (XAMPP)
    DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
    DB_PORT = int(os.getenv("DB_PORT", "3306"))
    DB_USER = os.getenv("DB_USER", "root")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "")
    DB_NAME = os.getenv("DB_NAME", "elearning_platform")

    @staticmethod
    def validate():
        """Check if all required credentials are set."""
        if not Settings.TELEGRAM_BOT_TOKEN:
            raise ValueError("❌ TELEGRAM_BOT_TOKEN not found in .env")
        if not Settings.GROQ_API_KEY:
            raise ValueError("❌ GROQ_API_KEY not found in .env")

        print("✅ All credentials loaded successfully!")
