User Preferences
----
  Finds a user's preferences

* **URL**

  `/profile/preferences`

* **Method:**

  `GET`

* **Success Response:**

  * **Code:** 200 <br />
    **Content:**

```json
{

}
```

* **Error Response:**

  * **Code:** 400 BAD <br />
    **Content:** `{"code":"BAD_REQUEST","message":"Decription of the error with payload"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


