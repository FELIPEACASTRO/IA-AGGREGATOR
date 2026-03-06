import '@testing-library/jest-dom';
import messages from './src/i18n/messages/pt-BR.json';

Object.defineProperty(window.HTMLElement.prototype, 'scrollIntoView', {
	writable: true,
	value: jest.fn(),
});

function resolveKey(path: string): string | undefined {
  const parts = path.split('.');
  let current: unknown = messages;
  for (const part of parts) {
    if (current && typeof current === 'object' && part in (current as Record<string, unknown>)) {
      current = (current as Record<string, unknown>)[part];
      continue;
    }
    return undefined;
  }
  return typeof current === 'string' ? current : undefined;
}

jest.mock('next-intl', () => ({
  useTranslations: () => (key: string, values?: Record<string, string | number>) => {
    const template = resolveKey(key) ?? key;
    if (!values) return template;
    return template.replace(/\{(\w+)\}/g, (_, token) => String(values[token] ?? `{${token}}`));
  },
}));
