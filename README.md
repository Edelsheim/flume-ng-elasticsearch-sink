# flume-ng-elasticsearch-sink
Flume-NG 1.9.0 Sink for Elasticsearch 7.13.3

# Requirements
* Flume-NG 1.9.0
* Elasticsearch 7.13.3

# Build
  Maven

```text

mvn build

mvn package

Copy To flume/lib

target/flume-ng-elasticsearch-sink-1.9.0.jar

and

target/lib/*
```

# Usage

```conf
agent.sinks = es
agent.sinks.es.type = elasticsearch
agent.sinks.es.channel = mem_channel
agent.sinks.es.clusterName = cluster-name
agent.sinks.es.hostNames = 127.0.0.1:9200,127.0.0.1:9300
agent.sinks.es.serializer = org.apache.flume.sink.elasticsearch.ElasticSearchDynamicSerializer
agent.sinks.es.indexName = flume
agent.sinks.es.indexType = message

```

  ----------------------------------------------------------------

  127.0.0.1:9200/_cat/indices?v

 check index **flume-yyyy-MM-dd**
