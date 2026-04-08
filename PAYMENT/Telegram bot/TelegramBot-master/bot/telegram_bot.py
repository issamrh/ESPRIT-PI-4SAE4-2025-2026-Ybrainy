"""
Telegram Bot - Handles Telegram commands and messages.
"""

from __future__ import annotations

import logging
import time
from typing import Any, Dict, List

import telebot
from requests.exceptions import ConnectionError, Timeout
from telebot import apihelper

from agent.agent import ChatAgent
from bot.pack_database import PackDatabaseService
from config.settings import Settings

# Configure API helper with better timeout settings
apihelper.SESSION_TIMEOUT = 10

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TelegramBot:
    """Telegram bot that uses our agent and MySQL data."""

    def __init__(self):
        """Initialize bot services and handlers."""
        self.agent = ChatAgent()
        self.pack_db = PackDatabaseService()
        self.bot = telebot.TeleBot(Settings.TELEGRAM_BOT_TOKEN)

        @self.bot.message_handler(commands=["start"])
        def handle_start(message):
            """Handle /start command."""
            welcome_message = (
                "Welcome to YBrainy E-Learning Platform Assistant.\n\n"
                "I can help with:\n"
                "- Course and certification questions\n"
                "- Learning guidance\n"
                "- Pack and category lookup from MySQL\n\n"
                "Commands:\n"
                "/packs - show packs from database\n"
                "/pack_categories - show pack categories from database"
            )
            self.bot.reply_to(message, welcome_message)

        @self.bot.message_handler(commands=["packs"])
        def handle_packs_command(message):
            """Handle /packs command."""
            self._reply_with_packs(message)

        @self.bot.message_handler(commands=["pack_categories"])
        def handle_pack_categories_command(message):
            """Handle /pack_categories command."""
            self._reply_with_pack_categories(message)

        @self.bot.message_handler(func=lambda message: True)
        def handle_message(message):
            """Handle incoming user messages."""
            try:
                text = (message.text or "").strip()
                self.bot.send_chat_action(message.chat.id, "typing")

                if self._is_pack_categories_question(text):
                    self._reply_with_pack_categories(message)
                    return

                if self._is_packs_question(text):
                    self._reply_with_packs(message)
                    return

                response = self.agent.process_message(text)
                self.bot.reply_to(message, response)
            except Exception:
                logger.exception("Error processing message")
                self.bot.reply_to(
                    message,
                    "Sorry, I encountered an error processing your message. Please try again.",
                )

    @staticmethod
    def _normalize_text(text: str) -> str:
        return " ".join((text or "").lower().split())

    def _is_packs_question(self, text: str) -> bool:
        normalized = self._normalize_text(text)
        phrases = (
            "show packs",
            "list packs",
            "available packs",
            "what packs",
            "packs available",
            "all packs",
            "pack list",
        )
        return normalized in {"packs", "pack"} or any(
            phrase in normalized for phrase in phrases
        )

    def _is_pack_categories_question(self, text: str) -> bool:
        normalized = self._normalize_text(text)
        phrases = (
            "pack categories",
            "show categories",
            "list categories",
            "categories of packs",
            "what categories",
        )
        return normalized in {"categories", "pack category"} or any(
            phrase in normalized for phrase in phrases
        )

    def _reply_with_packs(self, message) -> None:
        try:
            packs = self.pack_db.fetch_packs()
            response = self._format_packs_response(packs)
        except Exception as exc:
            logger.exception("Failed to load packs")
            response = (
                "Could not load packs from MySQL.\n"
                f"Error: {exc}\n"
                "Check DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, and DB_NAME in .env."
            )
        self._send_long_message(message.chat.id, response)

    def _reply_with_pack_categories(self, message) -> None:
        try:
            categories = self.pack_db.fetch_pack_categories()
            response = self._format_pack_categories_response(categories)
        except Exception as exc:
            logger.exception("Failed to load pack categories")
            response = (
                "Could not load pack categories from MySQL.\n"
                f"Error: {exc}\n"
                "Check DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, and DB_NAME in .env."
            )
        self._send_long_message(message.chat.id, response)

    def _send_long_message(self, chat_id: int, text: str, max_length: int = 3500) -> None:
        """
        Send text while respecting Telegram message size limits.
        """
        if len(text) <= max_length:
            self.bot.send_message(chat_id, text)
            return

        chunks: List[str] = []
        current: List[str] = []
        current_len = 0

        for block in text.split("\n\n"):
            block_len = len(block) + 2
            if current and current_len + block_len > max_length:
                chunks.append("\n\n".join(current))
                current = [block]
                current_len = block_len
            else:
                current.append(block)
                current_len += block_len

        if current:
            chunks.append("\n\n".join(current))

        for chunk in chunks:
            self.bot.send_message(chat_id, chunk)

    @staticmethod
    def _pick(row: Dict[str, Any], *keys: str, default: Any = "N/A") -> Any:
        for key in keys:
            if key in row and row[key] is not None and row[key] != "":
                return row[key]
        return default

    @staticmethod
    def _shorten(text: Any, limit: int = 120) -> str:
        if text is None:
            return "N/A"
        value = str(text).strip()
        if len(value) <= limit:
            return value
        return f"{value[:limit - 3]}..."

    @staticmethod
    def _format_price(value: Any) -> str:
        try:
            return f"${float(value):.2f}"
        except (TypeError, ValueError):
            return "N/A"

    def _format_packs_response(self, packs: List[Dict[str, Any]]) -> str:
        if not packs:
            return "No rows found in table packs."

        max_items = 20
        lines = [f"Packs found: {len(packs)}"]

        for idx, pack in enumerate(packs[:max_items], start=1):
            title = self._pick(pack, "title")
            category = self._pick(pack, "category_name", default="Uncategorized")
            level = self._pick(pack, "level")
            status = self._pick(pack, "status")
            duration = self._pick(pack, "duration_hours", "durationHours")
            sale_price = self._format_price(
                self._pick(pack, "sale_price", "salePrice", default=None)
            )
            original_price = self._format_price(
                self._pick(pack, "original_price", "originalPrice", default=None)
            )
            description = self._shorten(self._pick(pack, "description", default="N/A"))

            lines.append(
                f"{idx}. {title}\n"
                f"Category: {category} | Level: {level} | Status: {status}\n"
                f"Price: {sale_price} (original: {original_price}) | Duration hours: {duration}\n"
                f"Description: {description}"
            )

        if len(packs) > max_items:
            lines.append(f"... and {len(packs) - max_items} more packs.")

        return "\n\n".join(lines)

    def _format_pack_categories_response(self, categories: List[Dict[str, Any]]) -> str:
        if not categories:
            return "No rows found in table pack_categories."

        max_items = 25
        lines = [f"Pack categories found: {len(categories)}"]

        for idx, category in enumerate(categories[:max_items], start=1):
            name = self._pick(category, "name")
            status = self._pick(category, "status")
            icon = self._pick(category, "icon", default="-")
            description = self._shorten(self._pick(category, "description", default="N/A"))

            lines.append(
                f"{idx}. {name}\n"
                f"Status: {status} | Icon: {icon}\n"
                f"Description: {description}"
            )

        if len(categories) > max_items:
            lines.append(f"... and {len(categories) - max_items} more categories.")

        return "\n\n".join(lines)

    def test_connection(self) -> bool:
        """Test if bot can connect to Telegram API."""
        print("\nTesting connection to Telegram API...")
        max_retries = 3
        retry_delay = 2

        for attempt in range(1, max_retries + 1):
            try:
                bot_info = self.bot.get_me()
                print(f"Connection successful! Bot: @{bot_info.username}")
                print(f"Bot ID: {bot_info.id}")
                return True
            except (ConnectionError, Timeout) as exc:
                print(
                    f"Connection attempt {attempt}/{max_retries} failed: {type(exc).__name__}"
                )
                if attempt < max_retries:
                    print(f"Retrying in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                    retry_delay *= 2
                else:
                    print("Unable to connect to Telegram API after retries.")
                    print("Check internet access and bot token.")
                    return False
            except Exception as exc:
                print(f"Unexpected error: {exc}")
                return False

        return False

    def start(self) -> None:
        """Start the bot and keep it running."""
        print("Bot is starting...")

        if not self.test_connection():
            print("Cannot start bot without Telegram API connection.")
            return

        print("Press Ctrl+C to stop the bot")
        print("Waiting for incoming messages...\n")

        try:
            self.bot.infinity_polling()
        except KeyboardInterrupt:
            print("\nBot stopped by user.")
        except Exception as exc:
            print(f"\nError: {exc}")
            print(f"Error type: {type(exc).__name__}")
