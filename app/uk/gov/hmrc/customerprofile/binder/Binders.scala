/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customerprofile.binder

import eu.timepit.refined.api.{RefType, Validate}
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.Nino.isValid

object Binders {

  implicit def ninoBinder(implicit stringBinder: PathBindable[String]): PathBindable[Nino] = new PathBindable[Nino] {

    def unbind(
      key:  String,
      nino: Nino
    ): String = stringBinder.unbind(key, nino.value)

    def bind(
      key:   String,
      value: String
    ): Either[String, Nino] =
      if (isValid(value)) {
        Right(Nino(value))
      } else {
        Left("ERROR_NINO_INVALID")
      }
  }

  implicit def refinedQueryStringBindable[R[_, _], T, P](
    implicit
    baseTypeBinder: QueryStringBindable[T],
    refType:        RefType[R],
    validate:       Validate[T, P]
  ): QueryStringBindable[R[T, P]] = new QueryStringBindable[R[T, P]] {

    override def bind(
      key:    String,
      params: Map[String, Seq[String]]
    ): Option[Either[String, R[T, P]]] =
      baseTypeBinder
        .bind(key, params)
        .map(_.right.flatMap { baseValue =>
          refType.refine[P](baseValue)
        })

    override def unbind(
      key:   String,
      value: R[T, P]
    ): String =
      baseTypeBinder.unbind(key, refType.unwrap(value))
  }
}
