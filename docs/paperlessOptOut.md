Paperless Settings Opt Out
----
  Opts the user out of paperless

* **URL**

  `/profile/preferences/paperless-settings/opt-out`

* **Method:**

  `POST`

* **Success Response:**

  * **Code:** 200 <br />
    **Description:**

* **Error Response:**

  * **Code:** 400 BAD <br />
    **Content:** `{"code":"BAD_REQUEST","message":"JSON error flattened to a string describing the error that occured on the request"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


