"""
Telegram Bot - Handles all Telegram-related logic.

This module is responsible for:
1. Receiving messages from Telegram
2. Sending responses back
3. Handling basic commands like /start
"""

import time
import logging
import telebot
from telebot import apihelper
from requests.exceptions import ConnectionError, Timeout

from agent.agent import ChatAgent
from config.settings import Settings

# Configure API helper with better timeout settings
apihelper.SESSION_TIMEOUT = 10

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TelegramBot:
    """Telegram bot that uses our agent to respond to messages."""

    def __init__(self):
        """Initialize the bot and agent."""
        self.agent = ChatAgent()
        # Create bot instance with standard token
        self.bot = telebot.TeleBot(Settings.TELEGRAM_BOT_TOKEN)
        
        # Register handlers (what to do when something happens)
        @self.bot.message_handler(commands=['start'])
        def handle_start(message):
            """Handle /start command."""
            welcome_message = (
                "👋 Welcome to YBrainy E-Learning Platform Assistant!\n\n"
                "I'm your AI assistant dedicated to helping you with all things related to online learning and certifications.\n\n"
                "I can help you with:\n"
                "📚 Course information and recommendations\n"
                "🎓 Certification guidance and requirements\n"
                "💡 Learning tips and study strategies\n"
                "❓ Questions about the platform\n"
                "🏆 Progress and achievement tracking\n\n"
                "Feel free to ask me anything about e-learning! 🚀"
            )
            self.bot.reply_to(message, welcome_message)
        
        @self.bot.message_handler(func=lambda message: True)
        def handle_message(message):
            """Handle incoming user messages."""
            try:
                # Show typing indicator (nice UX touch)
                self.bot.send_chat_action(message.chat.id, 'typing')
                
                # Send message to our agent and get response
                response = self.agent.process_message(message.text)
                
                # Send response back to user
                self.bot.reply_to(message, response)
            except Exception as e:
                print(f"❌ Error processing message: {e}")
                self.bot.reply_to(message, "Sorry, I encountered an error processing your message. Please try again.")

    def test_connection(self) -> bool:
        """Test if bot can connect to Telegram API."""
        print("\n🔍 Testing connection to Telegram API...")
        max_retries = 3
        retry_delay = 2
        
        for attempt in range(1, max_retries + 1):
            try:
                # Try to get bot info - simplest API call
                bot_info = self.bot.get_me()
                print(f"✅ Connection successful! Bot: @{bot_info.username}")
                print(f"   Bot ID: {bot_info.id}")
                return True
            except (ConnectionError, Timeout) as e:
                print(f"⚠️  Connection attempt {attempt}/{max_retries} failed: {type(e).__name__}")
                if attempt < max_retries:
                    print(f"   Retrying in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                    retry_delay *= 2  # Exponential backoff
                else:
                    print(f"\n❌ Unable to connect to Telegram API after {max_retries} attempts.")
                    print("   Possible causes:")
                    print("   • No internet connection")
                    print("   • Firewall or network blocking")
                    print("   • VPN or proxy issues")
                    print("   • Invalid bot token in .env file")
                    print("   • Telegram servers temporarily unavailable")
                    return False
            except Exception as e:
                print(f"❌ Unexpected error: {e}")
                return False
        
        return False

    def start(self) -> None:
        """Start the bot and keep it running."""
        print("🤖 Bot is starting...")
        
        # Test connection before starting polling
        if not self.test_connection():
            print("\n⚠️  Cannot start bot without internet connection.")
            print("    Please check your network and try again.")
            return
        
        print("Press Ctrl+C to stop the bot")
        print("Waiting for incoming messages...\n")

        # Run the bot with simple error handling
        try:
            self.bot.infinity_polling()
        except KeyboardInterrupt:
            print("\n\n👋 Bot stopped by user.")
        except Exception as e:
            print(f"\n❌ Error: {e}")
            print(f"Error type: {type(e).__name__}")
