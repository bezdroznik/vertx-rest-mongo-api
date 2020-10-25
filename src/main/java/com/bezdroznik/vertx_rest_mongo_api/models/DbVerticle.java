package com.bezdroznik.vertx_rest_mongo_api.models;

import com.bezdroznik.vertx_rest_mongo_api.Encrypt;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DbVerticle extends AbstractVerticle {

  private MongoClient mongoClient;


  @Override
  public void start(Promise<Void> startPromise) {

    mongoClient = MongoConfig.mongoClientCongig(vertx);

    EventBus eventBus = vertx.eventBus();
    MessageConsumer<JsonObject> consumer = eventBus.consumer("persistence-address");

    consumer.handler(message -> {
      String action = message.body().getString("action");

      switch (action) {
        case "register-user":
          registerUser(message);
          break;
        case "login-user":
          login(message);
          break;
        case "add-user-item":
          addItem(message);
          break;
        case "get-user-items":
          getItems(message);
          break;
        default:
          System.out.println("CASE DEFAULT");
          message.fail(1, "Unkown action: " + message.body());
      }
    });
  }

  void registerUser(Message<JsonObject> message) {
    JsonObject user = message.body().getJsonObject("user");

    String hashedPassword = Encrypt.hashPassword(message.body().getJsonObject("user").getString("password"));
    JsonObject userToRegister = new JsonObject()
      .put("_id", user.getString("_id"))
      .put("login", user.getString("login"))
      .put("password", hashedPassword);

    saveInDb(new JsonObject().put("user", userToRegister));
    message.reply("Registering successfull.");

  }

  private void login(Message<JsonObject> message) {
    JsonObject user = message.body().getJsonObject("user");
    JsonObject query = new JsonObject().put("login", user.getString("login"));

    mongoClient.findOne("users", query, null, res -> {
      if (res.succeeded()) {
        String hashedPassword = res.result().getString("password");
        JsonObject replyMessage = new JsonObject()
          .put("passwordInformation", Encrypt.checkPassword(user.getString("password"), hashedPassword))
          .put("_id", res.result().getString("_id"));
        message.reply(replyMessage);
      }
    });

  }

  void addItem(Message<JsonObject> message) {
    JsonObject idMessage = new JsonObject()
      .put("action", "get-id")
      .put("token", message.body().getString("token"));

    vertx.eventBus().request("token-address", idMessage, ar -> {
      if (ar.succeeded() && !ar.result().body().toString().equals("token not found or expired")) {
        JsonObject item = new JsonObject()
          .put("_id", message.body().getJsonObject("item").getString("_id"))
          .put("name", message.body().getJsonObject("item").getString("name"))
          .put("owner", ar.result().body().toString());
        saveInDb(new JsonObject().put("item", item));
        message.reply("register user ended");
      } else {
        System.out.println("token not found or expired");
      }
    });


  }

  void getItems(Message<JsonObject> message) {
    JsonObject idMessage = new JsonObject()
      .put("action", "get-id")
      .put("token", message.body().getString("token"));

    vertx.eventBus().request("token-address", idMessage, ar -> {
      if (ar.succeeded() && !ar.result().body().toString().equals("token not found or expired")) {

        JsonObject query = new JsonObject().put("owner", ar.result().body().toString());

        mongoClient.find("items", query, res -> {
          JsonArray jsonArray = new JsonArray();
          res.result().forEach(jsonArray::add);

          if (res.succeeded()) {
            message.reply(jsonArray);
          }
        });
      } else {
        System.out.println("token not found or expired");
      }
    });

  }

  private void saveInDb(JsonObject jsonObject) {
    String mongoTableName;
    JsonObject product;
    if (jsonObject.containsKey("user")) {
      mongoTableName = "users";
      product = jsonObject.getJsonObject("user");
    } else {
      mongoTableName = "items";
      product = jsonObject.getJsonObject("item");
    }
    mongoClient.save(mongoTableName, product, res -> {
      if (res.succeeded()) {
        System.out.println("added " + product);
      } else {
        System.out.println("added failed " + product);
      }
    });
  }

}
