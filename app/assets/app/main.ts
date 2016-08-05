import { bootstrap } from '@angular/platform-browser-dynamic';
import { HTTP_PROVIDERS } from '@angular/http';
import { AppComponent } from './app.component';
import { enableProdMode } from '@angular/core';

if (window['mode'] == 'Prod') {
    enableProdMode();
}

bootstrap(AppComponent, [HTTP_PROVIDERS]);
