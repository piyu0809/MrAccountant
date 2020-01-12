package example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.util.Iterator;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;



public class Hello implements RequestStreamHandler
{
  private final String user = "username";
  private final String password = "password";
  private final String dbName = "dbname";
  private final String hostname = "mydbinstance2.cz7vhc9mxdlw.us-east-2.rds.amazonaws.com";
  private final String port = "5432";
  Statement readStatement = null;
  ResultSet resultSet = null;
  Statement setupStatement = null;
  String statement = null;
  JSONParser parser = new JSONParser();
  String category;
  String action;
  Double amt;
  String curr;

/** method to handle request and respose**/
   @Override
   public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      LambdaLogger logger = context.getLogger();
      logger.log("Loading Java Lambda handler of ProxyWithStream");
      String proxy = null;
      String param1 = null;
      String param2 = null;
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      JSONObject responseJson = new JSONObject();
      String responseCode = "200";
      JSONObject event = null;
      JSONObject body = null;
      String response = null;



       try {
           event = (JSONObject)parser.parse(reader);

           String s = (String) event.get("body");
           JSONObject json = (JSONObject) parser.parse(s);
           JSONObject qr  = (JSONObject) json.get("queryResult");
           JSONObject par = (JSONObject) qr.get("parameters");
           String category = (String) par.get("category");
           String action = (String) par.get("Action");

           String email_id = "piyu@gmail.com";
           String first_name = "piyu";
           String last_name = "Hiremath";
           if(action.equals("add")){

             JSONObject unit_curr = (JSONObject) par.get("unit-currency");
             Double   amt = (Double) unit_curr.get("amount");
             String curr = (String) unit_curr.get("currency");



             response = add_expense(email_id, first_name, last_name, category, amt, curr, true);
           }
           else if(action.equals("delete")){

             JSONObject unit_curr = (JSONObject) par.get("unit-currency");
             String curr = (String) unit_curr.get("currency");
             Double   amt = (Double) unit_curr.get("amount");
             System.out.println("amount: " + amt);

             amt = amt * -1;
             response = add_expense(email_id, first_name, last_name, category, amt, curr, false);
           }
           else if (action.equals("spend")){

               if (!par.get("date").equals("") && par.get("date-period").equals("")){
                 String date = (String) par.get("date");
                 String fdate = date.substring(0,10);
                 LocalDate ffdate = LocalDate.parse(fdate);
                 response = get_expenseToday(email_id, category, ffdate);
               }
               else if (!par.get("date-period").equals("") && par.get("date").equals("")) {

                 JSONObject dateperiod = (JSONObject) par.get("date-period");

                 String startDate = (String) dateperiod.get("startDate");

                 String endDate = (String) dateperiod.get("endDate");

                 String fstartDate = startDate.substring(0,10);

                 String fendDate = endDate.substring(0,10);

                 LocalDate ffstartDate = LocalDate.parse(fstartDate);

                 LocalDate ffendDate = LocalDate.parse(fendDate);

                 response = get_expensePeriod(email_id, category, ffstartDate, ffendDate);
               }
               else if(par.get("date").equals("") && par.get("date-period").equals("")){
                 response = get_expense(email_id, category);
               }
           }



       }
       catch(Exception pex)
       {
        responseJson.put("statusCode", "400");
        responseJson.put("exception", pex);
       }

           JSONObject responseBody = new JSONObject();
           responseBody.put("fulfillmentText",response);
           responseJson.put("statusCode", responseCode);
           responseJson.put("body", responseBody.toString());
           OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
           writer.write(responseJson.toJSONString());
           writer.close();


   }

/** retreiving expenses from database**/
   public String get_expense(String email_id, String category){

         String res = null;
         Connection conn = null;
         ResultSet resultSet = null;
         PreparedStatement st = null;
         String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;

         try {
     // Create connection to RDS DB instance
             Class.forName("org.postgresql.Driver");
             conn = DriverManager.getConnection(jdbcUrl);
             System.out.println("Connected to the PostgreSQL server successfully.");

            if(category.equals("")){
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=?");
              st.setString(1,email_id);
            }
            else{
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=? and Etype=?");
              st.setString(1,email_id);
              st.setString(2,category);
            }


             resultSet = st.executeQuery();
             while (resultSet.next())
             {
              res = resultSet.getString(1);
            }
            resultSet.close();
     }
       catch (SQLException ex) {
               // Handle any errors
               System.out.println("SQLException: " + ex.getMessage());
               System.out.println("SQLState: " + ex.getSQLState());
               System.out.println("VendorError: " + ex.getErrorCode());
       }
       catch (ClassNotFoundException e) {
               System.out.println(e.getMessage());
     }
        finally {
               System.out.println("Closing the connection.");
               if (conn != null)
               try {
                 conn.close();
               }
               catch (SQLException ignore) {}
       }

       if(res == null){
         return "you have spent 0 $";
       }
       return "you have spent " + res + " $";

   }

/** retreiving todays expenses from database for a particular period**/ 
   public String get_expensePeriod(String email_id, String category, LocalDate startDate, LocalDate endDate){
         System.out.println("Inside get_expensePeriod");
         System.out.println(email_id + " " + category+ " " +startDate+ " " +endDate);
         String res = null;
         Connection conn = null;
         ResultSet resultSet = null;
         PreparedStatement st = null;
         String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;

         try {
     // Create connection to RDS DB instance
             Class.forName("org.postgresql.Driver");
             conn = DriverManager.getConnection(jdbcUrl);
             System.out.println("Connected to the PostgreSQL server successfully.");
          //   readStatement = conn.createStatement();
            if(category.equals("")){
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=? and date between ? and ?");
              st.setString(1,email_id);
              st.setObject(2,startDate);
              st.setObject(3,endDate);
            }
            else{
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=? and Etype=? and date between ? and ?");
              st.setString(1,email_id);
              st.setString(2,category);
              st.setObject(3,startDate);
              st.setObject(4,endDate);
            }


             resultSet = st.executeQuery();
             while (resultSet.next())
             {
              res = resultSet.getString(1);
            }
            resultSet.close();
     }
       catch (SQLException ex) {
               // Handle any errors
               System.out.println("SQLException: " + ex.getMessage());
               System.out.println("SQLState: " + ex.getSQLState());
               System.out.println("VendorError: " + ex.getErrorCode());
       }
       catch (ClassNotFoundException e) {
               System.out.println(e.getMessage());
     }
        finally {
               System.out.println("Closing the connection.");
               if (conn != null)
               try {
                 conn.close();
               }
               catch (SQLException ignore) {}
       }
       if(res == null){
         return "you have spent 0 $";
       }
       return "you have spent " + res + " $";

   }

/** retrieving todays expenses from database**/
   public String get_expenseToday(String email_id, String category, LocalDate date){

         Connection conn = null;
         ResultSet resultSet = null;
         PreparedStatement st = null;
         String res = null;
         String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;

         try {
     		// Create connection to RDS DB instance
             Class.forName("org.postgresql.Driver");
             conn = DriverManager.getConnection(jdbcUrl);
             System.out.println("Connected to the PostgreSQL server successfully.");
            if(category.equals("")){
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=? and date=?");
              st.setString(1,email_id);
              st.setObject(2,date);
            }
            else{
              st = conn.prepareStatement("SELECT SUM(AMOUNT) from expenses where Email_ID=? and Etype=? and date=?");
              st.setString(1,email_id);
              st.setString(2,category);
              st.setObject(3,date);
            }

             resultSet = st.executeQuery();
             while (resultSet.next())
             {
                res = resultSet.getString(1);
            }
            resultSet.close();
     }
       catch (SQLException ex) {
               // Handle any errors
               System.out.println("SQLException: " + ex.getMessage());
               System.out.println("SQLState: " + ex.getSQLState());
               System.out.println("VendorError: " + ex.getErrorCode());
       }
       catch (ClassNotFoundException e) {
               System.out.println(e.getMessage());
     }
        finally {
               System.out.println("Closing the connection.");
               if (conn != null)
               try {
                 conn.close();
               }
               catch (SQLException ignore) {}
       }
       if(res == null){
         return "you have spent 0 $";
       }
       return "you have spent " +res+ " $";
   }

/** adding expenses to the database**/
   public String add_expense(String email_id, String first_name, String last_name, String category, Double amt, String curr, boolean Add){
         System.out.println("Inside Add");
         System.out.println(email_id + " " + first_name+ " " +last_name+ " " + category + " " + amt + " " + curr);
         Connection conn = null;
         String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;

         try {
     // Create connection to RDS DB instance
             Class.forName("org.postgresql.Driver");
             conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement st = conn.prepareStatement("INSERT INTO expenses (Email_ID, first_name, last_name, Date, Etype, Amount, Currency) VALUES (?, ?, ?, ?, ?, ?, ?)");
             st.setString(1, email_id);
             st.setString(2, first_name);
             st.setString(3, last_name);
             LocalDate now = LocalDate.now();
             st.setObject(4, now);
             st.setString(5, category);
             st.setDouble(6, amt);
             st.setString(7, curr);
             st.executeUpdate();
             st.close();


     }
       catch (SQLException ex) {
               // Handle any errors
               System.out.println("SQLException: " + ex.getMessage());
               System.out.println("SQLState: " + ex.getSQLState());
               System.out.println("VendorError: " + ex.getErrorCode());
       }
       catch (ClassNotFoundException e) {
               System.out.println(e.getMessage());
     }
        finally {
               System.out.println("Closing the connection.");
               if (conn != null)
               try {
                 conn.close();
               }
               catch (SQLException ignore) {}
       }
       if(Add){
         return "Successfully added " + amt +" $ " +  " to " + category;
       }
       return "Successfully deleted " + amt * -1 + " $ " + " from " + category;
   }

/** setting up SQL connection**/
   public Connection connect(String first, String last) {
         Connection conn = null;
         String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;

         try {
             Class.forName("org.postgresql.Driver");
             conn = DriverManager.getConnection(jdbcUrl);

             setupStatement = conn.createStatement();
             String SQL = "INSERT INTO Beanstalk (Resource) VALUES (?);";
             PreparedStatement statement = conn.prepareStatement(SQL);
             statement.setString(1,first);
             statement.addBatch();
             statement.setString(1,last);
             statement.addBatch();
             statement.executeBatch();


       }
       catch (SQLException ex) {
               // Handle any errors
               System.out.println("SQLException: " + ex.getMessage());
               System.out.println("SQLState: " + ex.getSQLState());
               System.out.println("VendorError: " + ex.getErrorCode());
       }
       catch (ClassNotFoundException e) {
               System.out.println(e.getMessage());
     }
        finally {
               System.out.println("Closing the connection.");
               if (conn != null)
               try {
                 conn.close();
               }
               catch (SQLException ignore) {}
       }



           return conn;
       }

}
