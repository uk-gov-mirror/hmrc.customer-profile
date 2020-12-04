Paperless Settings Opt Out
----
  Acts as a stub to the /profile/preferences/paperless-settings/opt-out endpoint.

  To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the 
  "X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220

* **URL**

  `/sandbox/profile/preferences/paperless-settings/opt-out`

* **Method:**

  `POST`

*  **URL Params**

   **Required:**
   `journeyId=[String]`

    a string which is included for journey tracking purposes but has no functional impact
    
*  **Request body**

```json
{}
```

* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:
  
  | *Value* | *HTTP Status Code* | *Description* 
  |---------|--------------------|---------------|
  | Not set or any value not specified below | 200 | Simulates a successful preference update. No body returned |
  | "PREFERENCE-CREATED" | 201 | Simulates a successful preference creation. No body returned |
  | "ERROR-401" | 401 | Triggers a 401 Unauthorized response |
  | "ERROR-403" | 403 | Triggers a 403 Forbidden response |
  | "ERROR-404" | 404 | Triggers a 404 NotFound response |
  | "ERROR-500" | 500 | Triggers a 500 Internal Server Error response |

* **Error Response:**

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`
