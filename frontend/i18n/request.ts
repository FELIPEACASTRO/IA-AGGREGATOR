import { getRequestConfig } from 'next-intl/server';
import messages from '../src/i18n/messages/pt-BR.json';

export default getRequestConfig(async () => ({
  locale: 'pt-BR',
  messages,
}));
