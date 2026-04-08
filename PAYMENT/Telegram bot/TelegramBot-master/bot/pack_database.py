"""
MySQL access for Telegram pack commands.
"""

from __future__ import annotations

from typing import Any, Dict, List

import mysql.connector
from mysql.connector import Error

from config.settings import Settings


class PackDatabaseService:
    """Reads pack and pack category data from MySQL."""

    def __init__(self) -> None:
        self._config = {
            "host": Settings.DB_HOST,
            "port": Settings.DB_PORT,
            "user": Settings.DB_USER,
            "password": Settings.DB_PASSWORD,
            "database": Settings.DB_NAME,
        }

    def fetch_packs(self) -> List[Dict[str, Any]]:
        """
        Fetch packs with category name.

        Uses p.* to stay resilient to schema naming differences.
        """
        query = """
            SELECT p.*, c.name AS category_name
            FROM packs p
            LEFT JOIN pack_categories c ON p.category_id = c.id
            ORDER BY p.id DESC
        """
        return self._fetch_all(query)

    def fetch_pack_categories(self) -> List[Dict[str, Any]]:
        """Fetch all pack categories."""
        query = """
            SELECT *
            FROM pack_categories
            ORDER BY id DESC
        """
        return self._fetch_all(query)

    def _fetch_all(self, query: str) -> List[Dict[str, Any]]:
        connection = None
        cursor = None
        try:
            connection = mysql.connector.connect(**self._config)
            cursor = connection.cursor(dictionary=True)
            cursor.execute(query)
            return cursor.fetchall()
        except Error as exc:
            raise RuntimeError(f"MySQL error: {exc}") from exc
        finally:
            if cursor is not None:
                cursor.close()
            if connection is not None and connection.is_connected():
                connection.close()
