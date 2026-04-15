import jsPDF from 'jspdf';

export function downloadAsPdf(title: string, content: string, filename: string): void {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' });
  const margin = 44;
  const maxWidth = 510;
  const lineHeight = 18;

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(16);
  doc.text(title, margin, margin);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);

  const lines = doc.splitTextToSize(content || 'No content available.', maxWidth);

  let y = margin + 30;
  lines.forEach((line: string) => {
    if (y >= 780) {
      doc.addPage();
      y = margin;
    }
    doc.text(line, margin, y);
    y += lineHeight;
  });

  doc.save(filename);
}
