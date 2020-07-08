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

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import fs2.Stream
import fs2.kafka.{ConsumerSettings, consumerResource}

class KafkaConsumer[F[_]: ConcurrentEffect: ContextShift: Timer, K, V](
    settings: ConsumerSettings[F, K, V],
    decoder: RecordDecoder[F, K, V])
    extends Consumer[F] {

  override def fetch(topic: String): Resource[F, Stream[F, Byte]] = {
    consumerResource[F]
      .using(settings)
      .evalTap(_.subscribeTo(topic))
      .map(_.stream.flatMap(decoder))
  }
}

object KafkaConsumer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer, K, V](
      settings: ConsumerSettings[F, K, V],
      decoder: RecordDecoder[F, K, V])
      : Consumer[F] =
    new KafkaConsumer(settings, decoder)
}
