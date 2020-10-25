package com.bezdroznik.vertx_rest_mongo_api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.UUID;

public class HttpVerticle extends AbstractVerticle {

  JWTAuth jwtAuth;

  @Override
  public void start(Promise<Void> startPromise) {

    jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setPassword("secret")));

    Router router = Router.router(vertx);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(3000);

    router.route("/*").handler(BodyHandler.create());
    router.post("/register").handler(this::registerUser);
    router.post("/login").handler(this::loginUser);
    router.get("/items").handler(JWTAuthHandler.create(jwtAuth)).handler(this::getUserItems);
    router.post("/logout").handler(JWTAuthHandler.create(jwtAuth)).handler(this::logout);
    router.post("/items").handler(JWTAuthHandler.create(jwtAuth)).handler(this::addItem);

  }

  private void logout(RoutingContext routingContext) {
    String token = routingContext.request().getHeader("Authorization").substring(7);

    JsonObject message = new JsonObject()
      .put("action", "delete-token")
      .put("token", token);

    vertx.eventBus().request("token-address", message, ar -> {
      if (ar.succeeded()) {
        routingContext.response()
          .headers().remove("Authorization");
        routingContext.response()
          .setStatusCode(204)
          .setStatusMessage("logout successfull")
          .end();
      } else {
        System.out.println("token not found or expired");
      }
    });
  }

  private void getUserItems(RoutingContext routingContext) {
    String token = routingContext.request().getHeader("Authorization").substring(7);

    JsonObject message = new JsonObject()
      .put("action", "get-user-items")
      .put("token", token);

    vertx.eventBus().request("persistence-address", message, ar -> {

      if (ar.succeeded()) {
        routingContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(ar.result().body()));
      } else {
        routingContext.response()
          .setStatusCode(401)
          .setStatusMessage("You have not provided an authentication token, the one provided has expired, was revoked or is not authentic.")
          .end();
      }
    });
  }

  private void loginUser(RoutingContext routingContext) {
    JsonObject user = routingContext.getBodyAsJson();

    JsonObject message = new JsonObject()
      .put("action", "login-user")
      .put("user", user);

    vertx.eventBus().request("persistence-address", message, ar -> {

      JsonObject parsedMessage = JsonObject.mapFrom(ar.result().body());

      if (ar.succeeded() && parsedMessage.getString("passwordInformation").equals("The password matches.")) {

        String token = jwtAuth.generateToken(new JsonObject()
            .put("_id", routingContext.getBodyAsJson().getString("_id"))
          , new JWTOptions().setIgnoreExpiration(true));

        JsonObject tokenObj = new JsonObject()
          .put("token", token)
          .put("_id", parsedMessage.getString("_id"))
          .put("expired", "false");
        addTokenToDb(tokenObj);

        routingContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(token));
      }
    });
  }

  private void addTokenToDb(JsonObject tokenObj) {

    JsonObject message = new JsonObject()
      .put("action", "save-token")
      .put("tokenObj", tokenObj);

    vertx.eventBus().request("token-address", message, ar -> {
      if (ar.succeeded()) {
        System.out.println("added token");
      } else {
        System.out.println("cannot add token");
      }
    });
  }

  private void registerUser(RoutingContext routingContext) {
    JsonObject user = routingContext.getBodyAsJson()
      .put("_id", UUID.randomUUID().toString());

    JsonObject message = new JsonObject()
      .put("action", "register-user")
      .put("user", user);

    vertx.eventBus().request("persistence-address", message, ar -> {

      if (ar.succeeded()) {
        routingContext.response()
          .setStatusCode(204)
          .setStatusMessage("Registering successfull")
          .end();
      }
    });
  }

  private void addItem(RoutingContext routingContext) {
    String token = routingContext.request().getHeader("Authorization").substring(7);

    JsonObject item = routingContext.getBodyAsJson()
      .put("_id", UUID.randomUUID().toString());

    JsonObject message = new JsonObject()
      .put("action", "add-user-item")
      .put("token", token)
      .put("item", item);

    vertx.eventBus().request("persistence-address", message, ar -> {

      if (ar.succeeded()) {
        routingContext.response()
          .setStatusCode(204)
          .setStatusMessage("Item created successfull.")
          .end();
      } else {
        routingContext.response()
          .setStatusCode(401)
          .setStatusMessage("You have not provided an authentication token, the one provided has expired, was revoked or is not authentic.")
          .end();
      }
    });
  }

}
