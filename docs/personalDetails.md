Personal Details
----
  Returns a user's designatory details

* **URL**

  `/profile/personal-details/{nino}`

* **Method:**

  `GET`

*  **URL Params**

   **Required:**

   `nino=[Nino]`

   The [NINO](https://github.com/hmrc/domain/blob/master/src/main/scala/uk/gov/hmrc/domain/Nino.scala#L21), National Insurance Number, given must be a valid NINO, ([http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm](http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm) / [Regular expression](https://github.com/hmrc/domain/blob/master/src/main/scala/uk/gov/hmrc/domain/Nino.scala#L36))

  **Required:**
   `journeyId=[String]`

    a string which is included for journey tracking purposes but has no functional impact

* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```json
{
  "person": {
    "firstName": "Jennifer",
    "lastName": "Thorsteinson",
    "title": "Ms",
    "sex": "Female",
    "dateOfBirth": 513774181935,
    "nino": "CS700100A"
  },
  "address": {
    "line1": "999 Big Street",
    "line2": "Worthing",
    "line3": "West Sussex",
    "postcode": "BN99 8IG"
  }
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


