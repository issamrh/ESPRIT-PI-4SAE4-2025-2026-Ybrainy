import { Injectable } from '@angular/core';

export type RuleSeverity = 'blocking' | 'warning' | 'tip';

export interface RuleViolation {
  id: string;
  severity: RuleSeverity;
  message: string;
  detail?: string;
}

export interface RulesResult {
  violations: RuleViolation[];
  blocking: RuleViolation[];
  warnings: RuleViolation[];
  tips: RuleViolation[];
  canSubmit: boolean;
}

@Injectable({ providedIn: 'root' })
export class ThreadRulesEngineService {

  analyze(title: string, body: string): RulesResult {
    const plainBody = body.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
    const violations: RuleViolation[] = [];

    // ── BLOCKING RULES ─────────────────────────────────────────────────────────

    // RULE_001: Title too short
    if (title.trim().length < 10) {
      violations.push({
        id: 'RULE_001',
        severity: 'blocking',
        message: 'Titre trop court',
        detail: `Le titre doit contenir au moins 10 caractères (actuellement ${title.trim().length}).`,
      });
    }

    // RULE_002: Body too short
    if (plainBody.length < 30) {
      violations.push({
        id: 'RULE_002',
        severity: 'blocking',
        message: 'Corps du fil trop court',
        detail: `Le corps doit contenir au moins 30 caractères (actuellement ${plainBody.length}).`,
      });
    }

    // RULE_003: Title is all caps
    if (title.trim().length >= 5 && title.trim() === title.trim().toUpperCase() && /[A-Z]/.test(title)) {
      violations.push({
        id: 'RULE_003',
        severity: 'blocking',
        message: 'Titre en majuscules',
        detail: 'Évitez d\'écrire le titre entièrement en majuscules.',
      });
    }

    // RULE_004: Title contains only special characters or numbers
    if (/^[\d\s\W]+$/.test(title.trim())) {
      violations.push({
        id: 'RULE_004',
        severity: 'blocking',
        message: 'Titre invalide',
        detail: 'Le titre doit contenir des mots significatifs.',
      });
    }

    // ── WARNING RULES ───────────────────────────────────────────────────────────

    // RULE_005: Title too long
    if (title.trim().length > 120) {
      violations.push({
        id: 'RULE_005',
        severity: 'warning',
        message: 'Titre trop long',
        detail: `Le titre dépasse 120 caractères (actuellement ${title.trim().length}). Essayez de le raccourcir.`,
      });
    }

    // RULE_006: Body very short (but not blocking)
    if (plainBody.length >= 30 && plainBody.length < 100) {
      violations.push({
        id: 'RULE_006',
        severity: 'warning',
        message: 'Corps trop bref',
        detail: 'Un corps plus détaillé aide les autres membres à mieux comprendre votre question.',
      });
    }

    // RULE_007: No question mark in body (might not be a question)
    if (!body.includes('?') && plainBody.length > 50) {
      violations.push({
        id: 'RULE_007',
        severity: 'warning',
        message: 'Aucune question posée',
        detail: 'Pensez à formuler votre question explicitement avec un point d\'interrogation.',
      });
    }

    // RULE_008: Repeated words in title (e.g. "help help help")
    const titleWords = title.trim().toLowerCase().split(/\s+/).filter(w => w.length > 2);
    const titleWordSet = new Set(titleWords);
    if (titleWords.length > 3 && titleWordSet.size < titleWords.length * 0.6) {
      violations.push({
        id: 'RULE_008',
        severity: 'warning',
        message: 'Mots répétés dans le titre',
        detail: 'Le titre semble contenir des mots répétés. Essayez d\'être plus précis.',
      });
    }

    // RULE_009: Body looks like it's just copy-pasted code (>80% non-alphabetic)
    const alphaRatio = (plainBody.match(/[a-zA-ZÀ-ÿ]/g) ?? []).length / (plainBody.length || 1);
    if (plainBody.length > 100 && alphaRatio < 0.3) {
      violations.push({
        id: 'RULE_009',
        severity: 'warning',
        message: 'Corps sans texte explicatif',
        detail: 'Le corps semble contenir principalement du code sans contexte. Ajoutez une description du problème.',
      });
    }

    // ── TIP RULES ───────────────────────────────────────────────────────────────

    // RULE_010: No category selected (we can't check here, but we'll let caller pass it)
    // (handled externally — skipped here since service doesn't know category state)

    // RULE_011: Body is short but acceptable — suggest expansion
    if (plainBody.length >= 100 && plainBody.length < 200) {
      violations.push({
        id: 'RULE_011',
        severity: 'tip',
        message: 'Corps court',
        detail: 'Envisagez d\'ajouter plus de contexte, de détails ou des exemples pour obtenir de meilleures réponses.',
      });
    }

    // RULE_012: Title doesn't end with ? for questions
    const questionWords = /^(comment|pourquoi|qu'est|quand|où|quel|quelle|quels|quelles|how|why|what|when|where|which|who|is|are|can|does)/i;
    if (questionWords.test(title.trim()) && !title.trim().endsWith('?')) {
      violations.push({
        id: 'RULE_012',
        severity: 'tip',
        message: 'Titre interrogatif sans point d\'interrogation',
        detail: 'Votre titre ressemble à une question. Pensez à ajouter un « ? » à la fin.',
      });
    }

    const blocking = violations.filter(v => v.severity === 'blocking');
    const warnings = violations.filter(v => v.severity === 'warning');
    const tips = violations.filter(v => v.severity === 'tip');

    return { violations, blocking, warnings, tips, canSubmit: blocking.length === 0 };
  }
}
