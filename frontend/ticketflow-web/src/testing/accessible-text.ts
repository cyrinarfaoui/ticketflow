/**
 * Le texte d'un élément TEL QU'UN LECTEUR D'ÉCRAN LE LIT : sans ce qui est marqué aria-hidden="true".
 * Indispensable avec les icônes Material Symbols : une icône est un MOT ("schedule", "arrow_forward")
 * que la police dessine en image ; textContent le verrait, un lecteur d'écran non.
 */
export function accessibleText(element: Element | null | undefined): string {
  if (!element) {
    return '';
  }
  const copy = element.cloneNode(true) as Element;
  copy.querySelectorAll('[aria-hidden="true"]').forEach((hidden) => hidden.remove());
  return (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
}
