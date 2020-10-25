package com.bezdroznik.vertx_rest_mongo_api;

import org.mindrot.jbcrypt.BCrypt;

public class Encrypt {

  public static String hashPassword(String plainTextPassword) {
    return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
  }

  public static String checkPassword(String plainPassword, String hashedPassword) {
    if (BCrypt.checkpw(plainPassword, hashedPassword)) {
      System.out.println("The password matches.");
      return "The password matches.";
    } else {
      System.out.println("The password does not match.");
      return "The password does not match.";
    }
  }
}
