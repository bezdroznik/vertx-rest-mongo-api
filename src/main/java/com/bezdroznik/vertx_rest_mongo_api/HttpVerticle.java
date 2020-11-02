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

    routingContext.response()
      .headers().remove("Authorization");
    routingContext.response()
      .setStatusCode(204)
      .setStatusMessage("logout successfull")
      .end();
  }

  private void getUserItems(RoutingContext routingContext) {

    jwtAuth.authenticate(new JsonObject()
      .put("jwt", routingContext.request().getHeader("Authorization").substring(7))
      .put("options", new JsonObject()), res -> {
      if (res.succeeded()) {
        String ownerId = res.result().principal().getString("_id");

        JsonObject message = new JsonObject()
          .put("action", "get-user-items")
          .put("owner", ownerId);

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
            .put("_id", JsonObject.mapFrom(ar.result().body()).getString("_id"))
          , new JWTOptions().setExpiresInMinutes(1));

        routingContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(token));
      } else {
        routingContext.response()
          .setStatusCode(401)
          .setStatusMessage("Wrong login or password.")
          .end();
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

      if (ar.result().body().toString().equals("Registering successfull.")) {
        routingContext.response()
          .setStatusCode(204)
          .setStatusMessage(ar.result().body().toString())
          .end();
      } else {
        routingContext.response()
          .setStatusCode(409)
          .setStatusMessage(ar.result().body().toString())
          .end();
      }
    });
  }

  private void addItem(RoutingContext routingContext) {

    jwtAuth.authenticate(new JsonObject()
      .put("jwt", routingContext.request().getHeader("Authorization").substring(7))
      .put("options", new JsonObject()), res -> {
      if (res.succeeded()) {
        String ownerId = res.result().principal().getString("_id");

        JsonObject item = routingContext.getBodyAsJson()
          .put("_id", UUID.randomUUID().toString())
          .put("owner", ownerId);

        JsonObject message = new JsonObject()
          .put("action", "add-user-item")
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
    });
  }
}
