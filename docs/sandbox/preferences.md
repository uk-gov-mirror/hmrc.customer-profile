User Preferences
----
  Acts as a stub to the /profile/preferences endpoint
  
  To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the 
  "X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220
  
* **URL**

  `/profile/preferences`

* **Method:**

  `GET`

*  **URL Params**

   **Required:**

   `nino=[Nino]`

   The [NINO](https://github.com/hmrc/domain/blob/master/src/main/scala/uk/gov/hmrc/domain/Nino.scala#L21), National Insurance Number, given must be a valid NINO, ([http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm](http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm) / [Regular expression](https://github.com/hmrc/domain/blob/master/src/main/scala/uk/gov/hmrc/domain/Nino.scala#L36))

   **Required:**
   `journeyId=[String]`

    a string which is included for journey tracking purposes but has no functional impact


* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:
  
  | *Value* | *HTTP Status Code* | *Description* 
  |---------|--------------------|---------------|
  | Not set or any value not specified below | 200 | Returns a response body with an example preferences json payload |
  | "VERIFIED" | 200 | Returns a response body with an example verified preferences json payload |
  | "UNVERIFIED" | 401 | Returns a response body with an example unverified preferences json payload |
  | "BOUNCED" | 401 | Returns a response body with an example bounced preferences json payload |
  | "REOPTIN" | 401 | Returns a response body with an example reOptIn preferences json payload |
  | "ERROR-401" | 401 | Triggers a 401 Unauthorized response |
  | "ERROR-403" | 403 | Triggers a 403 Forbidden response |
  | "ERROR-404" | 404 | Triggers a 404 NotFound response |
  | "ERROR-500" | 500 | Triggers a 500 Internal Server Error response |


* **Error Response:**

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`
