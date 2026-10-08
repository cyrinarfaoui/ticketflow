import { ApplicationConfig,LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { createInterceptorCondition, INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, includeBearerTokenInterceptor, provideKeycloak } from 'keycloak-angular';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { errorInterceptor } from './core/interceptors/error-interceptor';

const urlCondition = createInterceptorCondition({
    // Le jeton n'est joint QU'AUX appels vers notre gateway : jamais vers un site tiers
  urlPattern: /^http:\/\/localhost:8000(\/.*)?$/i,
  bearerPrefix: 'Bearer'
});

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, errorInterceptor])),
    provideKeycloak({
      config: {
        url: 'http://localhost:8081',
        realm: 'ticketflow',
        clientId: 'ticketflow-web'
      },
      initOptions: {
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
      }
    }),
    { provide: LOCALE_ID, useValue: 'fr' },
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: [urlCondition]
    }
  ]
};
