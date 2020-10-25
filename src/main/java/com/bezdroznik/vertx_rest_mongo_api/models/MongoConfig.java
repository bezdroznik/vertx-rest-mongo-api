package com.bezdroznik.vertx_rest_mongo_api.models;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoConfig {

  public static MongoClient mongoClientCongig(Vertx vertx) {
    return MongoClient.createShared(vertx, new JsonObject()
      .put("connection_string", "mongodb://localhost:27017")
      .put("db_name", "userTaskDB")
    );
  }

}
