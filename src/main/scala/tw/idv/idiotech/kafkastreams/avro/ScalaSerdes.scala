package tw.idv.idiotech.kafkastreams.avro

import com.sksamuel.avro4s.{ AvroSchema, Decoder, DefaultFieldMapper, Encoder, SchemaFor }
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.{ KafkaAvroDeserializer, KafkaAvroSerializer }
import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.common.serialization.{ Deserializer, Serde, Serializer }
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS
import org.apache.avro.Schema

trait KeySerde[T] extends Serde[T]
trait ValueSerde[T] extends Serde[T]

case class InnerSerdes(des: KafkaAvroDeserializer, ser: KafkaAvroSerializer) {

  def deserializer[T: Decoder](schema: Schema): Deserializer[T] =
    (topic: String, data: Array[Byte]) =>
      if (data == null) null.asInstanceOf[T]
      else
        des.deserialize(topic, data) match {
          case ir: IndexedRecord =>
            implicitly[Decoder[T]].decode(ir, schema, DefaultFieldMapper)
          case r => throw new IllegalArgumentException(s"unknown record: $r")
        }

  def serializer[T: Encoder: SchemaFor](schema: Schema): Serializer[T] =
    (topic: String, data: T) =>
      if (data == null) null
      else
        ser.serialize(topic, implicitly[Encoder[T]].encode(data, schema, DefaultFieldMapper))

}

trait ScalaSerdes {

  import scala.collection.JavaConverters._
  val schemaRegistry: String

  def serdesConfig =
    Map(SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistry, AUTO_REGISTER_SCHEMAS -> true).asJava

  def createInnerDes(isKey: Boolean): KafkaAvroDeserializer

  def createInnerSer(isKey: Boolean): KafkaAvroSerializer

  lazy val innerKeySerdes = InnerSerdes(createInnerDes(true), createInnerSer(true))
  lazy val innerValueSerdes = InnerSerdes(createInnerDes(false), createInnerSer(false))

  def keySerde[T : Encoder: Decoder: SchemaFor]: KeySerde[T] = new KeySerde[T] {
    override def serializer(): Serializer[T] = innerKeySerdes.serializer(AvroSchema[T])
    override def deserializer(): Deserializer[T] = innerKeySerdes.deserializer(AvroSchema[T])
  }

  def valueSerde[T : Encoder: Decoder: SchemaFor]: ValueSerde[T] = new ValueSerde[T] {
    override def serializer(): Serializer[T] = innerValueSerdes.serializer(AvroSchema[T])
    override def deserializer(): Deserializer[T] = innerValueSerdes.deserializer(AvroSchema[T])
  }

}

trait ProductionSerdes extends ScalaSerdes {

  override def createInnerDes(isKey: Boolean): KafkaAvroDeserializer = {
    val ret = new KafkaAvroDeserializer()
    ret.configure(serdesConfig, isKey)
    ret
  }

  override def createInnerSer(isKey: Boolean): KafkaAvroSerializer = {
    val ret = new KafkaAvroSerializer()
    ret.configure(serdesConfig, isKey)
    ret
  }
}

trait MockSchemaRegistry extends ScalaSerdes {
  override lazy val schemaRegistry: String = "fak"
  private val client = new MockSchemaRegistryClient()
  override def createInnerDes(isKey: Boolean): KafkaAvroDeserializer =
    new KafkaAvroDeserializer(client, serdesConfig)
  override def createInnerSer(isKey: Boolean): KafkaAvroSerializer =
    new KafkaAvroSerializer(client, serdesConfig)
}
