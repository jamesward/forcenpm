import { Component, OnInit } from '@angular/core';
import { HTTP_PROVIDERS } from '@angular/http';
import { AutoCompleteDirective, AutoCompleteComponent } from 'auto-complete';

import { AppService, UserOrgInfo } from './app.service';


declare var __moduleName : string;

@Component({
    moduleId: __moduleName,
    selector: 'my-app',
    templateUrl: 'app.component.html',
    providers: [AppService],
    directives: [AutoCompleteDirective, AutoCompleteComponent]
})

export class AppComponent implements OnInit {

    private userOrgInfo: UserOrgInfo = new UserOrgInfo();
    private instanceUrl: string = window['instanceUrl'];
    private logoutUrl: string = window['logoutUrl'];
    private sldsUrl: string = window['sldsUrl'];
    private npmSearchUrl: string = '/npm/search';

    private npmPackageName;
    private npmPackageVersion: string;


    private npmPackageVersions = [];

    private forceNpmList = [];

    private forceNpmFileList = [];

    private selectedForceNpm = {};

    constructor(private appService: AppService) { }

    fetchForceNpm() {
        this.appService.forceNpmList().forEach(forceNpmList => {
            this.forceNpmList = forceNpmList.map( forceNpm => {
                let descriptionParts = forceNpm['Description'].replace('NPM Package: ', '').split(' ');
                let name = descriptionParts[0];
                let version = descriptionParts[1];
                return { name: name, version: version };
            });
        });
    }

    ngOnInit() {
        this.appService.getUserInfo().forEach(userOrgInfo => this.userOrgInfo = userOrgInfo);
        this.fetchForceNpm();
    }

    npmPackageNameSelected(packageInfo) {
        this.appService.npmPackageVersions(packageInfo.value).forEach(versions => this.npmPackageVersions = versions);
    }

    createNpmResource() {
        // todo: spinner
        let createPromise = this.appService.forceNpmCreate(this.npmPackageName.value, this.npmPackageVersion);
        createPromise.forEach( response => {
            this.fetchForceNpm();
            // todo: spinner
        });
        this.npmPackageName = null;
        this.npmPackageVersion = null;
    }

    visualforceFileReferences(forceNpm) {
        this.selectedForceNpm = forceNpm;
        this.forceNpmFileList = [];
        if (forceNpm.name != undefined) {
            this.appService.forceNpmFileList(forceNpm).forEach(forceNpmFileList => this.forceNpmFileList = forceNpmFileList);
        }
    }

}
