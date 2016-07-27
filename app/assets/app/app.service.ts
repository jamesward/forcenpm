import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions, URLSearchParams } from '@angular/http';

import 'rxjs/add/operator/map';

@Injectable()
export class AppService {

    constructor(private http: Http) {
        this.http._defaultOptions.headers.append('X-ID-URL', window['idUrl']);
        this.http._defaultOptions.headers.append('X-ACCESS-TOKEN', window['accessToken']);
        this.http._defaultOptions.headers.append('X-REFRESH-TOKEN', window['refreshToken']);
        this.http._defaultOptions.headers.append('X-INSTANCE-URL', window['instanceUrl']);
    }

    getUserInfo() {
        return this.http.get('/force/userinfo')
            .map(response => response.json() as UserOrgInfo);
    }

    forceNpmList() {
        return this.http.get('/force/npm')
            .map(response => response.json());
    }

    forceNpmCreate(packageName, packageVersion) {
        let params = new URLSearchParams();
        params.set('name', packageName);
        params.set('version', packageVersion);
        let options = new RequestOptions({ search: params });
        return this.http.post('/force/npm', null, options)
            .map(response => response.json());
    }

    npmPackageVersions(packageName) {
        return this.http.get('/npm/versions?name=' + packageName)
            .map(response => response.json());
    }
}

export class UserOrgInfo {
    Name: string;
    OrganizationType: string;
    username: string;
}
