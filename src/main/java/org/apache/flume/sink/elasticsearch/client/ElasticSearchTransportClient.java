/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.sink.elasticsearch.client;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.sink.elasticsearch.ElasticSearchEventSerializer;
import org.apache.flume.sink.elasticsearch.ElasticSearchIndexRequestBuilderFactory;
import org.apache.flume.sink.elasticsearch.IndexNameBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.FieldMapper.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.apache.flume.sink.elasticsearch.ElasticSearchSinkConstants.DEFAULT_PORT;

public class ElasticSearchTransportClient implements ElasticSearchClient {

  public static final Logger logger = LoggerFactory
      .getLogger(ElasticSearchTransportClient.class);

  private TransportAddress[] serverAddresses;

  private ElasticSearchEventSerializer serializer;
  private ElasticSearchIndexRequestBuilderFactory indexBuilder;

  //private BulkRequestBuilder bulkRequestBuilder;
  private BulkRequest bulkRequestBuilder;

  //private Client client;
  private RestHighLevelClient client;

  @VisibleForTesting
  TransportAddress[] getServerAddresses() {
    return serverAddresses;
  }

  @VisibleForTesting
  void setBulkRequestBuilder(BulkRequest bulkRequestBuilder) {
    this.bulkRequestBuilder = bulkRequestBuilder;
  }

  /**
   * Transport client for external cluster
   * 
   * @param hostNames
   * @param clusterName
   * @param serializer
   */
  public ElasticSearchTransportClient(String[] hostNames, String clusterName,
      ElasticSearchEventSerializer serializer) {
    configureHostnames(hostNames);
    openClient(clusterName);
    this.serializer = serializer;
  }

  public ElasticSearchTransportClient(String[] hostNames, String clusterName,
      ElasticSearchIndexRequestBuilderFactory indexBuilder) {
    configureHostnames(hostNames);
    openClient(clusterName);
    this.indexBuilder = indexBuilder;
  }
  
  /**
   * Local transport client only for testing
   * 
   * @param indexBuilderFactory
   */
  public ElasticSearchTransportClient(ElasticSearchIndexRequestBuilderFactory indexBuilderFactory) {
    this.indexBuilder = indexBuilderFactory;
    openLocalDiscoveryClient();
  }
  
  /**
   * Local transport client only for testing
   *
   * @param serializer
   */
  public ElasticSearchTransportClient(ElasticSearchEventSerializer serializer) {
    this.serializer = serializer;
    openLocalDiscoveryClient();
  }

  /**
   * Used for testing
   *
   * @param client
   *    ElasticSearch Client
   * @param serializer
   *    Event Serializer
   */
  public ElasticSearchTransportClient(RestHighLevelClient client,
      ElasticSearchEventSerializer serializer) {
    this.client = client;
    this.serializer = serializer;
  }

  /**
   * Used for testing
   */
  public ElasticSearchTransportClient(RestHighLevelClient client,
                                      ElasticSearchIndexRequestBuilderFactory requestBuilderFactory)
      throws IOException {
    this.client = client;
   // requestBuilderFactory.createIndexRequest(client, null, null, null);
  }

  private void configureHostnames(String[] hostNames) {
    logger.warn(Arrays.toString(hostNames));
    serverAddresses = new TransportAddress[hostNames.length];
    for (int i = 0; i < hostNames.length; i++) {
      String[] hostPort = hostNames[i].trim().split(":");
      String host = hostPort[0].trim();
      int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1].trim())
              : DEFAULT_PORT;
      serverAddresses[i] = new TransportAddress(new InetSocketAddress(host, port));
    }
  }
  
  @Override
  public void close() {
    if (client != null) {
      try {
		client.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
    }
    client = null;
  }

  @Override
  public void addEvent(Event event, IndexNameBuilder indexNameBuilder,
      String indexType, long ttlMs) throws Exception {
    if (bulkRequestBuilder == null) {
	    bulkRequestBuilder = new BulkRequest();
    }

    XContentBuilder content = this.serializer.getContentBuilder(event);
    content.endObject();

	  IndexRequest request = new IndexRequest(indexNameBuilder.getIndexName(event));
    request.source(content);
    bulkRequestBuilder.add(request);
  }

  @Override
  public void execute() throws Exception {
    try {
      client.bulk(bulkRequestBuilder, RequestOptions.DEFAULT);
    } finally {
      bulkRequestBuilder = new BulkRequest();
    }
  }

  /**
   * Open client to elaticsearch cluster
   * 
   * @param clusterName
   */
  private void openClient(String clusterName) {
    logger.info("Using ElasticSearch hostnames: {} ",
        Arrays.toString(serverAddresses));
    
    HttpHost[] hosts = new HttpHost[serverAddresses.length];

    for(int i = 0; i < serverAddresses.length; i++) {
    	TransportAddress host = serverAddresses[i];
    	hosts[i] = new HttpHost(host.getAddress(), host.getPort(), "http");
    }
    RestHighLevelClient transportClient = new RestHighLevelClient(
    		RestClient.builder(hosts));
    if (client != null) {
      try {
		client.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
    }
    client = transportClient;
  }

  /*
   * FOR TESTING ONLY...
   * 
   * Opens a local discovery node for talking to an elasticsearch server running
   * in the same JVM
   */
  private void openLocalDiscoveryClient() {
  }

  @Override
  public void configure(Context context) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
