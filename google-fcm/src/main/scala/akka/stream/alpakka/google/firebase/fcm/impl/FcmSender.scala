/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.google.firebase.fcm.impl

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.alpakka.google.firebase.fcm.{FcmErrorResponse, FcmResponse, FcmSuccessResponse}
import akka.annotation.InternalApi
import akka.stream.alpakka.google.GoogleSettings
import akka.stream.alpakka.google.http.GoogleHttp
import spray.json._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] class FcmSender {
  import FcmJsonSupport._

  def send(http: GoogleHttp, fcmSend: FcmSend)(
      implicit materializer: Materializer,
      settings: GoogleSettings
  ): Future[FcmResponse] = {
    val projectId = settings.projectId
    val url = s"https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

    val response = http.singleRequestWithOAuth(
      HttpRequest(
        HttpMethods.POST,
        url,
        immutable.Seq(),
        HttpEntity(ContentTypes.`application/json`, fcmSend.toJson.compactPrint)
      )
    )
    parse(response)
  }

  private def parse(response: Future[HttpResponse])(implicit materializer: Materializer): Future[FcmResponse] = {
    implicit val executionContext: ExecutionContext = materializer.executionContext
    response.flatMap { rsp =>
      if (rsp.status.isSuccess) {
        Unmarshal(rsp.entity).to[FcmSuccessResponse]
      } else {
        Unmarshal(rsp.entity).to[FcmErrorResponse]
      }
    }
  }
}
