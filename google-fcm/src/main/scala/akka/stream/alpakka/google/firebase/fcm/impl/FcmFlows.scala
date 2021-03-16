/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.google.firebase.fcm.impl
import akka.NotUsed
import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.stream.Materializer
import akka.stream.alpakka.google.GoogleSettings
import akka.stream.alpakka.google.firebase.fcm._
import akka.stream.alpakka.google.http.GoogleHttp
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] object FcmFlows {

  private[fcm] def fcmWithData[T](sender: FcmSender, maxConcurrentConnections: Int, isTest: Boolean)(
      implicit settings: GoogleSettings
  ): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val mat: Materializer = materializer
        implicit val system: ActorSystem = materializer.system
        val http = GoogleHttp()

        Flow[(FcmNotification, T)]
          .mapAsync(maxConcurrentConnections)(
            in =>
              sender
                .send(http, FcmSend(isTest, in._1))
                .zip(Future.successful(in._2))
          )
      }
      .mapMaterializedValue(_ => NotUsed)

  private[fcm] def fcm(sender: FcmSender, maxConcurrentConnections: Int, isTest: Boolean)(
      implicit settings: GoogleSettings
  ): Flow[FcmNotification, FcmResponse, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val mat: Materializer = materializer
        implicit val system: ActorSystem = materializer.system
        val http = GoogleHttp()

        val sender: FcmSender = new FcmSender()
        Flow[FcmNotification]
          .mapAsync(maxConcurrentConnections)(
            in => sender.send(http, FcmSend(isTest, in))
          )
      }
      .mapMaterializedValue(_ => NotUsed)
}
