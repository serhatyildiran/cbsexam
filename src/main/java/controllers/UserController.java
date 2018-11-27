package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.auth0.jwt.exceptions.JWTDecodeException;
import model.User;
import utils.Hashing;
import utils.Log;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class UserController {

  private static DatabaseController dbCon;

  public UserController() {
    dbCon = new DatabaseController();
  }

  public static User getUser(int id) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where id=" + id;

    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User user = null;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        user =
                new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));

        // return the create object
        return user;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return null
    return user;
  }

  /**
   * Get all users in database
   *
   * @return
   */
  public static ArrayList<User> getUsers() {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL
    String sql = "SELECT * FROM user";

    // Do the query and initialyze an empty list for use if we don't get results
    ResultSet rs = dbCon.query(sql);
    ArrayList<User> users = new ArrayList<User>();

    try {
      // Loop through DB Data
      while (rs.next()) {
        User user =
                new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));

        // Add element to list
        users.add(user);
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return the list of users
    return users;
  }

  public static User createUser(User user) {

    // Gets the hashing
    Hashing hashing = new Hashing();

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);

    // Set creation time for user.
    user.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the user in the DB
    // TODO: Hash the user password before saving it : FIX
    int userID = dbCon.insert(
            "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES('"
                    + user.getFirstname()
                    + "', '"
                    + user.getLastname()
                    + "', '"
                    + hashing.HashingSalt(user.getPassword())
                    + "', '"
                    + user.getEmail()
                    + "', "
                    + user.getCreatedTime()
                    + ")");

    if (userID != 0) {
      //Update the userid of the user before returning
      user.setId(userID);
    } else {
      // Return null if user has not been inserted into database
      return null;
    }

    // Return user
    return user;
  }

  // Method to delete a user
  public static Boolean deleteUser(String token) {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      DecodedJWT jwt = JWT.decode(token);
      int id = jwt.getClaim("user_id").asInt();

      try {
        PreparedStatement deleteUser = dbCon.getConnection().prepareStatement("DELETE FROM user WHERE id = ?");

        deleteUser.setInt(1, id);

        int rowAffected = deleteUser.executeUpdate();

        if (rowAffected == 1) {
          return true;
        }

      } catch (SQLException sql) {
        sql.printStackTrace();
      }

    } catch (JWTDecodeException ex) {
      ex.printStackTrace();
    }

    return false;


  }

  // Method to update a user
  public static Boolean updateUser(User user, String token) {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      DecodedJWT jwt = JWT.decode(token);
      int id = jwt.getClaim("user_id").asInt();

      try {
        PreparedStatement updateUser = dbCon.getConnection().prepareStatement("UPDATE user SET" +
                "first_name = ?, last_name = ?, password = ?, email = ? WHERE id=? ");


        updateUser.setString(1, user.getFirstname());
        updateUser.setString(2, user.getLastname());
        updateUser.setString(3, user.getPassword());
        updateUser.setString(4, user.getEmail());
        updateUser.setInt(5, id);

        int rowsAffected = updateUser.executeUpdate();

        if (rowsAffected == 1) {
          return true;
        }

        } catch (SQLException sql) {
        sql.printStackTrace();
        }

      } catch (JWTDecodeException ex) {
        ex.printStackTrace();
      }

      return false;
    }
  }
