package com.bezdroznik.vertx_rest_mongo_api.models;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class JwtVerticle extends AbstractVerticle {

  private MongoClient mongoClient;

  @Override
  public void start() throws Exception {
    mongoClient = MongoConfig.mongoClientCongig(vertx);

    EventBus eventBus = vertx.eventBus();
    MessageConsumer<JsonObject> consumer = eventBus.consumer("token-address");
    consumer.handler(message -> {
      String action = message.body().getString("action");

      switch (action) {
        case "save-token":
          saveToken(message);
          break;
        case "delete-token":
          deleteToken(message);
          break;
        case "expire-token":
          expireToken(message);
          break;
        case "get-id":
          getId(message);
          break;
        default:
          System.out.println("CASE DEFAULT");
          message.fail(1, "Unkown action: " + message.body());
      }
    });

  }

  private void saveToken(Message<JsonObject> message) {
    JsonObject tokenObj = message.body().getJsonObject("tokenObj");

    mongoClient.save("tokens", tokenObj, res -> {
      if (res.succeeded()) {
        message.reply("added: " + tokenObj);
      } else {
        message.reply("added failed: " + tokenObj);
      }
    });
  }

  private void deleteToken(Message<JsonObject> message) {
    JsonObject tokenObj = new JsonObject().put("token", message.body().getString("token"));

    mongoClient.findOneAndDelete("tokens", tokenObj, res -> {
      if (res.succeeded()) {
        message.reply("token deleted correctly");
      } else {
        message.reply("token not found or expired");
      }

    });
  }

  private void expireToken(Message<JsonObject> message) {
    JsonObject tokenObj = message.body().getJsonObject("tokenObj");

    JsonObject query = new JsonObject().put("token", tokenObj.getString("token"));
    JsonObject update = new JsonObject()
      .put("token", tokenObj.getString("token"))
      .put("_id", tokenObj.getString("_id"))
      .put("expired", "true");

    mongoClient.findOneAndUpdate("tokens", query, update, res -> {
    });
  }

  private void getId(Message<JsonObject> message) {
    JsonObject query = new JsonObject().put("token", message.body().getString("token"));

    mongoClient.findOne("tokens", query, null, res -> {
      if (res.succeeded()) {
        message.reply(res.result().getString("_id"));
      } else {
        message.reply("token not found or expired");
      }
    });
  }

}
