export const environment = {
  production: false,
  // All requests go through the Angular dev-server proxy → API Gateway (8088).
  apiBaseUrl: '',
  courseApiBaseUrl: '',
  partnerApiBaseUrl: '',
  apiUrl: '/api',
  cartApiUrl: '/api',
  financeApiUrl: '/api/finance',
  forumApiUrl: '',
  forumWsUrl: '',
  googleIdpHint: 'google',
  // Use localhost for browser-based auth flows. `host.docker.internal` is meant for
  // containers to reach the host, and often does not resolve from the host browser.
  keycloakUrl: 'http://localhost:9190',
  keycloakRealm: 'microservices',
  keycloakClientId: 'angular-client',
  twelveDataApiKey: 'REPLACE_WITH_YOUR_TWELVE_DATA_API_KEY',
};
