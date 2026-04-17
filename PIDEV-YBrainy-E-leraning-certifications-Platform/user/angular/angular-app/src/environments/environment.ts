export const environment = {
  production: false,
  // In dev we rely on the Angular dev-server proxy (proxy.conf.json) to avoid CORS.
  apiBaseUrl: '',
  courseApiBaseUrl: '',
  partnerApiBaseUrl: '',
  apiUrl: 'http://localhost:8095/api',
  cartApiUrl: 'http://localhost:8954/api',
  financeApiUrl: 'http://localhost:8995/api/finance',
  forumApiUrl: '',
  forumWsUrl: '',
  googleIdpHint: 'google',
  keycloakUrl: 'http://localhost:9190',
  keycloakRealm: 'microservices',
  keycloakClientId: 'angular-client',
};
