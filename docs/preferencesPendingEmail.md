Preferences Pending Email for paperless settings.
----
  Sets a pending email for paperless settings email address updates.

* **URL**

  `/profile/preferences/pending-email`

* **Method:**

  `PUT`

*  **Request body**

```json
{"email" : "example@email.com"}
```

* **Success Response:**

  * **Code:** 200 <br />
    **Description:** The email address preference has been updated

* **Error Response:**

  * **Code:** 400 BAD REQUEST<br />
    **Content:** `{"code":"BAD_REQUEST","message":"JSON error flattened to a string describing the error that occured on the request"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"code":"NOT_FOUND","message":"Resource was not found"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`
    
  * **Code:** 409 CONFLICT <br />
    **Content:** `{"code":"CONFLICT","message":"No existing verified or pending data"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


