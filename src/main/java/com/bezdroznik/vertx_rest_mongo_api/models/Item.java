package com.bezdroznik.vertx_rest_mongo_api.models;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class Item {

  UUID id;
  UUID owner;
  String name;

  public JsonObject toJson() {

    JsonObject item = new JsonObject()
      .put("_id", this.id.toString())
      .put("owner", this.owner.toString())
      .put("name", this.name);

    return new JsonObject().put("item", item);
  }
}
