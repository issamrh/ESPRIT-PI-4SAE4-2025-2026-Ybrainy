import { AtsResult } from '../types';

const STOP_WORDS = new Set([
  'about', 'above', 'after', 'again', 'against', 'along', 'among', 'and', 'are', 'because', 'been',
  'before', 'being', 'below', 'between', 'both', 'could', 'doing', 'each', 'from', 'have', 'into',
  'just', 'more', 'most', 'other', 'over', 'same', 'some', 'such', 'that', 'their', 'there', 'these',
  'they', 'this', 'those', 'under', 'very', 'what', 'when', 'where', 'which', 'while', 'with', 'your',
]);

function extractKeywords(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, ' ')
    .split(/\s+/)
    .map((word) => word.trim())
    .filter((word) => word.length >= 4 && !STOP_WORDS.has(word));
}

export function computeAtsScore(jobDescription: string, cvText: string): AtsResult {
  const jobKeywords = extractKeywords(jobDescription);
  const cvBody = cvText.toLowerCase();

  const uniqueKeywords = Array.from(new Set(jobKeywords));
  if (uniqueKeywords.length === 0) {
    return { score: 0, matchedKeywords: [], missingKeywords: [] };
  }

  const matchedKeywords = uniqueKeywords.filter((keyword) => cvBody.includes(keyword));
  const missingKeywords = uniqueKeywords.filter((keyword) => !cvBody.includes(keyword));

  const score = Math.min(100, Math.round((matchedKeywords.length / uniqueKeywords.length) * 100));

  return {
    score,
    matchedKeywords: matchedKeywords.slice(0, 10),
    missingKeywords: missingKeywords.slice(0, 10),
  };
}
