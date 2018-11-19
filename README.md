# CUSTOMER-PROFILE

[![Build Status](https://travis-ci.org/hmrc/customer-profile.svg?branch=master)](https://travis-ci.org/hmrc/customer-profile) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customer-profile/images/download.svg) ](https://bintray.com/hmrc/releases/customer-profile/_latestVersion)

Allows users to view tax profile and communication preferences
 

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```.


API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/profile/accounts``` | GET | Look up for the current user's tax account identifiers. [More...](docs/accounts.md)  |
| ```/profile/personal-details/:nino``` | GET | Returns a user's designatory details. [More...](docs/personalDetails.md)  |
| ```/profile/preferences``` | GET | Returns the user's preferences. [More...](docs/preferences.md)|
| ```/profile/preferences/paperless-settings/opt-in``` | POST | Sets or updates the user's paperless opt-in preference settings. [More...](docs/paperlessSettingsOptIn.md)|
| ```/profile/preferences/paperless-settings/opt-out``` | POST | Opts the user out of paperless. [More...](docs/paperlessSettingsOptOut.md)|

# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint, e.g.
```
    GET /sandbox/profile/accounts
```

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" to specify the appropriate status code and return payload. 
See each linked file for details:

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/sandbox/profile/accounts``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/accounts.md)  |
| ```/sandbox/profile/personal-details/:nino``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/personalDetails.md)  |
| ```/sandbox/profile/preferences``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/preferences.md)|
| ```/sandbox/profile/preferences/paperless-settings/opt-in``` | POST | Acts as a stub for the related live endpoint. [More...](docs/sandbox/paperlessSettingsOptIn.md)|
| ```/sandbox/profile/preferences/paperless-settings/opt-out``` | POST | Acts as a stub for the related live endpoint. [More...](docs/sandbox/paperlessSettingsOptOut.md)|



# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
