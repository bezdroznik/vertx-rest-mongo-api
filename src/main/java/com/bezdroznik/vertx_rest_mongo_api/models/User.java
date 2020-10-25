package com.bezdroznik.vertx_rest_mongo_api.models;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class User {

  UUID id;
  String login;
  String password;

  public JsonObject toJson() {

    JsonObject user = new JsonObject()
      .put("_id", this.getId().toString())
      .put("login", this.login)
      .put("password", this.password);

    return new JsonObject().put("user", user);
  }
}
