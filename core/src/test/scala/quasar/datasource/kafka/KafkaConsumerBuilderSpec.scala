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

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

import cats.effect.IO
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset, ConsumerRecord}
import quasar.EffectfulQSpec
import quasar.datasource.kafka.TestImplicits._

class KafkaConsumerBuilderSpec extends EffectfulQSpec[IO] {

  "KafkaDecoders" >> {
    "RawKey drops the value, preserves the key" >>* {
      val decoder = KafkaConsumerBuilder.RawKey[IO]
      val record = CommittableConsumerRecord[IO, Array[Byte], Array[Byte]](
        ConsumerRecord("precog", 0, 0, "key".getBytes, "value".getBytes),
        CommittableOffset(new TopicPartition("precog", 0), new OffsetAndMetadata(0), None, _ => IO.unit))
      decoder(record).compile.toList.map(_ mustEqual "key".getBytes.toList)
    }

    "RawValue drops the key, preserves the value" >>* {
      val decoder = KafkaConsumerBuilder.RawValue[IO]
      val record = CommittableConsumerRecord[IO, Array[Byte], Array[Byte]](
        ConsumerRecord("precog", 0, 0, "key".getBytes, "value".getBytes),
        CommittableOffset(new TopicPartition("precog", 0), new OffsetAndMetadata(0), None, _ => IO.unit))
      decoder(record).compile.toList.map(_ mustEqual "value".getBytes.toList)
    }
  }

}
