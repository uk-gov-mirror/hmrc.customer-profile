User Tax Account Profile
----
  Acts as a stub to the /profile/accounts endpoint.
  
  To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the 
  "X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220
  
* **URL**

  `/sandbox/profile/accounts`

* **Method:**

  `GET`

*  **URL Params**

   **Optional:**
   `journeyId=[String]`

    an optional string which may be included for journey tracking purposes but has no functional impact


* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:
  
  | *Value* | *HTTP Status Code* | *Description* 
  |---------|--------------------|---------------|
  | Not set or any value not specified below | 200 | Returns a response body with an example accounts json payload |
  | "ERROR-401" | 401 | Triggers a 401 Unauthorized response |
  | "ERROR-403" | 403 | Triggers a 403 Forbidden response |
  | "ERROR-500" | 500 | Triggers a 500 Internal Server Error response |


* **Error Response:**

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`
