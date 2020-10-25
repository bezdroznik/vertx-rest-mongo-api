package com.bezdroznik.vertx_rest_mongo_api;

import com.bezdroznik.vertx_rest_mongo_api.models.DbVerticle;
import com.bezdroznik.vertx_rest_mongo_api.models.JwtVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName());

  }

  @Override
  public void start(Promise<Void> startPromise) {

    deployVerticle(HttpVerticle.class.getName());
    deployVerticle(DbVerticle.class.getName());
    deployVerticle(JwtVerticle.class.getName());
  }


  Promise<Void> deployVerticle(String verticleName) {
    Promise<Void> retVal = Promise.promise();
    vertx.deployVerticle(verticleName, event -> {
      if (event.succeeded()) {
        retVal.complete();
      } else {
        retVal.fail(event.cause());
      }
    });
    return retVal;
  }

}
