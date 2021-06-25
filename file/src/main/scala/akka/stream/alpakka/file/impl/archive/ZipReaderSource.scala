/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.file.impl.archive

import akka.NotUsed
import akka.annotation.InternalApi
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.util.ByteString
import java.io.InputStream;
import java.util.zip.{ZipEntry, ZipInputStream}

case class ZipArchiveMetadata(name: String)

@InternalApi class ZipSource(in: InputStream, chunkSize: Int)
    extends GraphStage[SourceShape[(ZipArchiveMetadata, ByteString)]] {
  private val out = Outlet[(ZipArchiveMetadata, ByteString)]("flowOut")
  override val shape: SourceShape[(ZipArchiveMetadata, ByteString)] =
    SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      val zis = new ZipInputStream(in)

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            val e = zis.getNextEntry
            if (e != null) {
              val n = ZipArchiveMetadata(e.getName)
              val data = new Array[Byte](chunkSize)
              val c = zis.read(data, 0, chunkSize)
              zis.closeEntry()
              if (c == -1) {
                completeStage()
              } else {
                push(out, Pair(n, ByteString.fromArray(data, 0, c)))
              }
            } else {
              zis.close()
              completeStage()
            }
          }
        }
      )

      override def postStop(): Unit = {
        super.postStop()
        zis.close()
      }
    }
}
