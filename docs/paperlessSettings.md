Paperless Settings
----
  Sets or Updates the user's paperless preference settings.

  The acceptance is based off the user being shown and agreeing to, these [Terms and Conditions](https://www.tax.service.gov.uk/information/terms#secure)

* **URL**

  `/profile/preferences/paperless-settings`

* **Method:**

  `POST`

*  **Request body**

```json
    {
        "generic": {
            "accepted":true
        },
        "email": "name@email.co.uk"
    }
```

* **Success Response:**

  * **Code:** 200 <br />
    **Description:** Update to an existing record
  * **Code:** 201 <br />
    **Description:** Created a new record

* **Error Response:**

  * **Code:** 400 BAD <br />
    **Content:** `{"code":"BAD_REQUEST","message":"JSON error flattened to a string describing the error that occured on the request"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


