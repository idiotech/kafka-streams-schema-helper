name := "kafka-streams-schema-helper"
organization := "tw.idv.idiotech"
version := "0.0.1"

scalaVersion := "2.12.10"

val thirdPartyRepos = Seq(
  "confluent-release" at "https://packages.confluent.io/maven/"
)
resolvers := (thirdPartyRepos ++ resolvers.value)

val confluentAvroVersion = "5.3.2"
val kafkaVersion = "2.3.1"

val testDependencies = Seq(
  "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion,
)

libraryDependencies ++= Seq(
  "io.confluent" % "kafka-avro-serializer" % confluentAvroVersion,
  "io.confluent" % "kafka-streams-avro-serde" % confluentAvroVersion,
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.9",
) ++ testDependencies.map(_ % Test)

dependencyOverrides ++= Seq(
  "org.apache.kafka" % "kafka-clients" % kafkaVersion
)

