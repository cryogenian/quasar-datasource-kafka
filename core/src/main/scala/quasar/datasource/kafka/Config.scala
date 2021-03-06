/*
 * Copyright 2020 Precog Data
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

package quasar.datasource.kafka

import slamdata.Predef._

import argonaut.Argonaut._
import argonaut._
import cats.data.NonEmptyList
import cats.syntax.either._
import quasar.connector.DataFormat

case class Config(
  bootstrapServers: NonEmptyList[String],
  groupId: String,
  topics: NonEmptyList[String],
  decoder: Decoder,
  format: DataFormat
) extends Product with Serializable {
  def isTopic(topic: String): Boolean = topics.exists(_ == topic)

  // No sensitive information at this time
  def sanitize: Config = this
  def reconfigure(patch: Config): Either[Config, Config] = patch.asRight
}

object Config {

  // We implement the encoder instead of using argonaut's because we implement the decoder
  implicit def nelEJ[A: EncodeJson]: EncodeJson[NonEmptyList[A]] = _.toList.asJson

  // We implement our on decoder because Argonaut's default decoder
  // error message doesn't say the problem is that the list is empty
  implicit def nelDJ[A: DecodeJson]: DecodeJson[NonEmptyList[A]] = c => c.as[List[A]].flatMap {
    case Nil    =>
      c.history.head match {
        case Some(El(CursorOpDownField(f), _)) =>
          DecodeResult.fail(s"$f value cannot be an empty array", c.history)
        case _ =>
          DecodeResult.fail("empty list found where non-empty list expected", c.history)
      }
    case h :: t =>
      DecodeResult.ok(NonEmptyList(h, t))
  }

  implicit val configCodec: CodecJson[Config] = CodecJson({ (config: Config) =>
    ("bootstrapServers" := config.bootstrapServers) ->:
      ("groupId" := config.groupId) ->:
      ("topics" := config.topics) ->:
      ("decoder" := config.decoder) ->:
      config.format.asJson
  }, (c => for {
    bootstrapServers <- (c --\ "bootstrapServers").as[NonEmptyList[String]]
    groupId <- (c --\ "groupId").as[String]
    topics <- (c --\ "topics").as[NonEmptyList[String]]
    decoder <- (c --\ "decoder").as[Decoder]
    format <- c.as[DataFormat]
  } yield Config(bootstrapServers, groupId, topics, decoder, format)))

}
