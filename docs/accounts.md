User Tax Accounts
----
  Look up for the current user's tax account identifiers, e.g. National Insurance Number (NINO), Self Assessment UTR, etc

* **URL**

  `/profile/accounts`

* **Method:**

  `GET`

*  **URL Params**

   N/A

* **Success Response:**

  * **Code:** 200 <br />
    **Content:**

```json
{

}
```

* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


