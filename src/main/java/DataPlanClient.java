/*
 * Copyright 2015, gRPC Authors All rights reserved.
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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the
 */
public class DataPlanClient {
  private static final Logger logger = Logger.getLogger(DataPlanClient.class.getName());

  private final ManagedChannel channel;
  private final DataPlanServiceGrpc.DataPlanServiceBlockingStub blockingStub;

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public DataPlanClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build());
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  DataPlanClient(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = DataPlanServiceGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }


  public static UserParamRequest createUserParamRequest(){
    double[] usages = new double[30];
    Date[] dates = new Date[30];

    UserParamRequest.Builder builder = UserParamRequest.newBuilder();
    Date today = new Date();
    for(int i = 0; i < 30; i++) {
      usages[i] = (i % 7) * 10 + Math.random() * 1;
      Date date = new Date(today.getTime() + (1000 * 60 * 60 * 24 * i));
      dates[i] = date;
      Usage usage = Usage.newBuilder().setUsage(usages[i]).setDay(date.getDay()).setMonth(date.getMonth()).setYear(date.getYear()).build();
      builder.addUsages(usage);
    }
    builder.setOverage(0.01);


    return builder.build();
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws Exception {
    DataPlanClient client = new DataPlanClient("rpc.chenxi.io", 50051);
    try {
      UserParamRequest request = createUserParamRequest();
      UserParams response = client.blockingStub.getUserParam(request);
      System.out.println("Response from Server: ");
      System.out.println(response);
    } finally {
      client.shutdown();
    }
  }
}
