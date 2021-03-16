/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.google.firebase.fcm.scaladsl

import akka.actor.ActorSystem
import akka.stream.alpakka.google.auth.ServiceAccountCredentials
import akka.stream.alpakka.google.{GoogleSettings, RequestSettings, RetrySettings}
import akka.stream.alpakka.google.firebase.fcm.impl.{FcmFlows, FcmSender}
import akka.stream.alpakka.google.firebase.fcm.{FcmNotification, FcmResponse, FcmSettings}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object GoogleFcm {
  private val scopes: Seq[String] = Seq(
    "https://www.googleapis.com/auth/firebase.messaging"
  )

  def sendWithPassThrough[T](maxConcurrentConnections: Int, isTest: Boolean)(
      implicit settings: GoogleSettings
  ): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] = {
    FcmFlows.fcmWithData[T](new FcmSender, maxConcurrentConnections, isTest)
  }

  @deprecated("use sendWithPassThrough[T](maxConcurrentConnections: Int, isTest: Boolean) instead", "3.0.0")
  def sendWithPassThrough[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] = {
    import scala.compat.java8.OptionConverters._
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val system: ActorSystem = materializer.system
        val confRoot = system.settings.config.getConfig(GoogleSettings.ConfigPath)
        implicit val settings: GoogleSettings = GoogleSettings.create(
          conf.projectId,
          ServiceAccountCredentials(conf.projectId, conf.clientEmail, conf.privateKey, scopes),
          RequestSettings(confRoot),
          RetrySettings(confRoot.getConfig("retry-settings")),
          conf.forwardProxy.map(_.toCommonForwardProxy).asJava
        )
        FcmFlows.fcmWithData[T](new FcmSender, conf.maxConcurrentConnections, conf.isTest)
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  def send(maxConcurrentConnections: Int,
           isTest: Boolean)(implicit settings: GoogleSettings): Flow[FcmNotification, FcmResponse, NotUsed] = {
    FcmFlows.fcm(new FcmSender, maxConcurrentConnections, isTest)
  }

  @deprecated("use send(maxConcurrentConnections: Int, isTest: Boolean) instead", "3.0.0")
  def send(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] = {
    import scala.compat.java8.OptionConverters._
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val system: ActorSystem = materializer.system
        val confRoot = system.settings.config.getConfig(GoogleSettings.ConfigPath)
        implicit val settings: GoogleSettings = GoogleSettings.create(
          conf.projectId,
          ServiceAccountCredentials(conf.projectId, conf.clientEmail, conf.privateKey, scopes),
          RequestSettings(confRoot),
          RetrySettings(confRoot.getConfig("retry-settings")),
          conf.forwardProxy.map(_.toCommonForwardProxy).asJava
        )
        FcmFlows.fcm(new FcmSender, conf.maxConcurrentConnections, conf.isTest)
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  def fireAndForget(maxConcurrentConnections: Int,
                    isTest: Boolean)(implicit settings: GoogleSettings): Sink[FcmNotification, Future[Done]] = {
    FcmFlows.fcm(new FcmSender, maxConcurrentConnections, isTest).toMat(Sink.ignore)(Keep.right)
  }

  @deprecated("use fireAndForget(maxConcurrentConnections: Int, isTest: Boolean) instead", "3.0.0")
  def fireAndForget(conf: FcmSettings): Sink[FcmNotification, Future[Done]] = {
    import scala.compat.java8.OptionConverters._
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val system: ActorSystem = materializer.system
        val confRoot = system.settings.config.getConfig(GoogleSettings.ConfigPath)
        implicit val settings: GoogleSettings = GoogleSettings.create(
          conf.projectId,
          ServiceAccountCredentials(conf.projectId, conf.clientEmail, conf.privateKey, scopes),
          RequestSettings(confRoot),
          RetrySettings(confRoot.getConfig("retry-settings")),
          conf.forwardProxy.map(_.toCommonForwardProxy).asJava
        )
        FcmFlows.fcm(new FcmSender, conf.maxConcurrentConnections, conf.isTest)
      }
      .toMat(Sink.ignore)(Keep.right)
  }

}
