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


* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```json
{
  "etag" : "etag12345",
  "person" : {
    "firstName" : "John",
    "middleName" : "Albert",
    "lastName" : "Smith",
    "title" : "Mr",
    "honours" : "BSC",
    "sex" : "M",
    "dateOfBirth" : 513774181935,
    "nino" : "YH261980B"
  },
  "address" : {
    "line1": "1 Test Street",
    "line2": "Testington",
    "line3": "Testville",
    "line4": "Testshire",
    "postcode:" : "AA1 1AA",
    "country:" : "Test Country",
    "startDate": 1509556398243,
    "type": "Residential"
  },
  "correspondenceAddress" : {
    "line1" : "1 Main Street",
    "line2" : "Central",
    "line3" : "Anothertown",
    "line4" : "Anothershire",
    "postcode" : "BB2 2BB",
    "country:" : "Test Country",
    "startDate": 1341100800000,
    "type" : "Correspondence"
  }
}
```

* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{"code":"BAD_REQUEST","message":"Bad Request"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


