User Preferences
----
  Finds a user's preferences

* **URL**

  `/profile/preferences`

* **Method:**

  `GET`

*  **URL Params**

   **Optional:**
   `journeyId=[String]`

    an optional string which may be included for journey tracking purposes but has no functional impact

* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```json
{
  "digital" : true,
  "email" : {
    "email" : "name@email.co.uk",
    "status" : "verified"
  }
}
```

```json
{
  "digital" : true,
  "email" : {
    "email" : "name@email.co.uk",
    "status" : "pending",
    "linkSent": "2020-06-04"
  }
}
```

```json
{
  "digital" : false
}
```


* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"Some auth message"}`

  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


