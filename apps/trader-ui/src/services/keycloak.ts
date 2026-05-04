import Keycloak from 'keycloak-js';

/**
 * Keycloak client setup for OIDC. In Phase 3 (US1) the dev profile of
 * the OMS service is unauthenticated, so this client is optional. Phase 14
 * (prod-shadow) flips the OMS into OAuth2 Resource Server mode and the UI
 * MUST authenticate.
 */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'swiss-tms',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'trader-ui',
});

export async function tryInitKeycloak(): Promise<boolean> {
  if (import.meta.env.VITE_KEYCLOAK_DISABLED === 'true') {
    return false;
  }
  try {
    return await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
    });
  } catch (e) {
    // Phase 3 dev mode runs without Keycloak. Don't block the UI.
    console.warn('Keycloak init failed, continuing unauthenticated:', e);
    return false;
  }
}
