export function isOrcaMobile(): boolean {
    return typeof window !== 'undefined' && typeof window.OrcaMobile !== 'undefined';
}
