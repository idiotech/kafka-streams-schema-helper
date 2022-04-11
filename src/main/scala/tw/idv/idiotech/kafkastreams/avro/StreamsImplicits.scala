package tw.idv.idiotech.kafkastreams.avro

import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Joined, Materialized, Produced}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer, Serdes => JSerdes}
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.state.KeyValueStore

trait StreamsImplicits {
  import StreamsImplicits._

  implicit def stringSerde: KeyValueSerde[String] = JSerdes.String().toKeyValueSerde()
  implicit def longSerde: KeyValueSerde[Long] = JSerdes.Long().asInstanceOf[Serde[Long]].toKeyValueSerde()
  implicit def javaLongSerde: KeyValueSerde[java.lang.Long] = JSerdes.Long().toKeyValueSerde()
  implicit def byteArraySerde: KeyValueSerde[Array[Byte]] = JSerdes.ByteArray().toKeyValueSerde()
  implicit def bytesSerde: KeyValueSerde[org.apache.kafka.common.utils.Bytes] = JSerdes.Bytes().toKeyValueSerde()
  implicit def floatSerde: KeyValueSerde[Float] = JSerdes.Float().asInstanceOf[Serde[Float]].toKeyValueSerde()
  implicit def javaFloatSerde: KeyValueSerde[java.lang.Float] = JSerdes.Float().toKeyValueSerde()
  implicit def doubleSerde: KeyValueSerde[Double] = JSerdes.Double().asInstanceOf[Serde[Double]].toKeyValueSerde()
  implicit def javaDoubleSerde: KeyValueSerde[java.lang.Double] = JSerdes.Double().toKeyValueSerde()
  implicit def integerSerde: KeyValueSerde[Int] = JSerdes.Integer().asInstanceOf[Serde[Int]].toKeyValueSerde()
  implicit def javaIntegerSerde: KeyValueSerde[java.lang.Integer] = JSerdes.Integer().toKeyValueSerde()
  implicit def booleanSerde: KeyValueSerde[Boolean] = new KeyValueSerde[Boolean] {
    val intSerde = JSerdes.Integer()
    override def serializer(): Serializer[Boolean] =
      (topic: String, data: Boolean) => intSerde.serializer().serialize(topic, if (data) 1 else 0)
    override def deserializer(): Deserializer[Boolean] =
      (topic: String, data: Array[Byte]) => intSerde.deserializer().deserialize(topic, data) > 0
  }

  implicit def groupedFromSerde[K, V](
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V]
  ): Grouped[K, V] =
    Grouped.`with`[K, V]

  implicit def consumedFromSerde[K, V](
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V]
  ): Consumed[K, V] =
    Consumed.`with`[K, V]

  implicit def producedFromSerde[K, V](
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V]
  ): Produced[K, V] =
    Produced.`with`[K, V]

  implicit def materializedFromSerde[K, V, S <: StateStore](
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V]
  ): Materialized[K, V, S] =
    Materialized.`with`[K, V, S]

  implicit def joinedFromKeyValueOtherSerde[K, V, VO](
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V],
    otherValueSerde: ValueSerde[VO]
  ): Joined[K, V, VO] =
    Joined.`with`[K, V, VO]

  def materializeAs[K, V](name: String)(
    implicit keySerde: KeySerde[K],
    valueSerde: ValueSerde[V]
  ): Materialized[K, V, KeyValueStore[Bytes, Array[Byte]]] =
    Materialized.as[K, V, KeyValueStore[Bytes, Array[Byte]]](name)
}
object StreamsImplicits {
  trait KeyValueSerde[T] extends KeySerde[T] with ValueSerde[T]
  implicit class RichSerde[T](serde: Serde[T]) {
    def toKeyValueSerde(): KeyValueSerde[T] = new KeyValueSerde[T] {
      override def serializer(): Serializer[T] = serde.serializer()
      override def deserializer(): Deserializer[T] = serde.deserializer()
    }
  }
}
