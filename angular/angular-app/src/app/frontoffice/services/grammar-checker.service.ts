import { Injectable } from '@angular/core';

export interface GrammarIssue {
  id: string;
  type: 'spelling' | 'punctuation' | 'style';
  message: string;
  example?: string;
}

export interface GrammarResult {
  issues: GrammarIssue[];
  score: number; // 0–100, 100 = no issues
}

@Injectable({ providedIn: 'root' })
export class GrammarCheckerService {

  check(text: string): GrammarResult {
    const plain = text.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    const issues: GrammarIssue[] = [];

    // ── French common mistakes ─────────────────────────────────────────────────

    // G001 — "a" vs "à"
    if (/\ba cause\b/i.test(plain)) {
      issues.push({ id: 'G001', type: 'spelling', message: 'Confusion « a » / « à »', example: '« a cause » → « à cause »' });
    }

    // G002 — "ou" vs "où"
    if (/\bou est\b/i.test(plain) || /\bou sont\b/i.test(plain) || /\bou se trouve\b/i.test(plain)) {
      issues.push({ id: 'G002', type: 'spelling', message: 'Confusion « ou » / « où »', example: '« ou est » → « où est »' });
    }

    // G003 — "sa" vs "ça"
    if (/\bsa marche\b/i.test(plain) || /\bsa ne\b/i.test(plain) || /\bsa fait\b/i.test(plain)) {
      issues.push({ id: 'G003', type: 'spelling', message: 'Confusion « sa » / « ça »', example: '« sa marche » → « ça marche »' });
    }

    // G004 — "c'est" vs "s'est"
    if (/\bc est\b/i.test(plain)) {
      issues.push({ id: 'G004', type: 'spelling', message: 'Apostrophe manquante', example: '« c est » → « c\'est »' });
    }

    // G005 — missing space before "?" or "!"
    if (/\w[?!]/.test(plain)) {
      issues.push({ id: 'G005', type: 'punctuation', message: 'Espace manquant avant « ? » ou « ! »', example: 'En français, on met une espace avant ? et !' });
    }

    // G006 — double spaces
    if (/  /.test(plain)) {
      issues.push({ id: 'G006', type: 'style', message: 'Doubles espaces détectés', example: 'Supprimez les espaces en trop.' });
    }

    // G007 — all lowercase (no capital at start of sentence)
    const sentences = plain.split(/[.!?]+/).map(s => s.trim()).filter(s => s.length > 5);
    const noCapital = sentences.filter(s => /^[a-zàâäéèêëîïôùûüÿ]/.test(s));
    if (noCapital.length > 0 && noCapital.length >= sentences.length * 0.5) {
      issues.push({ id: 'G007', type: 'style', message: 'Phrases sans majuscule initiale', example: 'Commencez chaque phrase par une majuscule.' });
    }

    // G008 — "est ce que" (missing hyphen)
    if (/\best ce que\b/i.test(plain) || /\best ce qu\b/i.test(plain)) {
      issues.push({ id: 'G008', type: 'spelling', message: '« est-ce que » mal orthographié', example: '« est ce que » → « est-ce que »' });
    }

    // G009 — "peut etre" (missing hyphen)
    if (/\bpeut etre\b/i.test(plain)) {
      issues.push({ id: 'G009', type: 'spelling', message: '« peut-être » mal orthographié', example: '« peut etre » → « peut-être »' });
    }

    // G010 — "d'accord" vs "daccord"
    if (/\bdaccord\b/i.test(plain)) {
      issues.push({ id: 'G010', type: 'spelling', message: '« d\'accord » mal orthographié', example: '« daccord » → « d\'accord »' });
    }

    // G011 — "parce que" written as "parsque" or "pasque"
    if (/\bpasque\b/i.test(plain) || /\bparsque\b/i.test(plain)) {
      issues.push({ id: 'G011', type: 'spelling', message: '« parce que » mal orthographié', example: '« pasque/parsque » → « parce que »' });
    }

    // G012 — missing period at end of body
    const lastChar = plain.charAt(plain.length - 1);
    if (plain.length > 20 && !'.!?…'.includes(lastChar)) {
      issues.push({ id: 'G012', type: 'punctuation', message: 'Ponctuation finale manquante', example: 'Terminez votre texte par un point, un « ? » ou un « ! ».' });
    }

    // Score: 100 minus 10 per issue, floor at 0
    const score = Math.max(0, 100 - issues.length * 10);

    return { issues, score };
  }
}
