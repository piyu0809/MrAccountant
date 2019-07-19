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
  private final String user = "piyu";
  private final String password = "Piyu#0809";
  private final String dbName = "dbname";
  private final String hostname = "mydbinstance2.cz6vhc4mxdlw.us-east-2.rds.amazonaws.com";
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
           System.out.println(event.toString());
           if((event.get("body")).getClass().equals(String.class)){
             System.out.println("String");

           }
           System.out.println("Body: " + event.get("body"));

           String s = (String) event.get("body");
           JSONObject json = (JSONObject) parser.parse(s);
           JSONObject qr  = (JSONObject) json.get("queryResult");
           JSONObject par = (JSONObject) qr.get("parameters");
           String category = (String) par.get("category");
           System.out.println("Category: " + category);
           String action = (String) par.get("Action");
           System.out.println("Action: " + action);

           String email_id = "piyu@gmail.com";
           String first_name = "piyu";
           String last_name = "Hiremath";
           if(action.equals("add")){
             System.out.println("Actions add");

             System.out.println("unit_curr not null");
             JSONObject unit_curr = (JSONObject) par.get("unit-currency");
             Double   amt = (Double) unit_curr.get("amount");
             System.out.println("amount: " + amt);
             String curr = (String) unit_curr.get("currency");
            System.out.println("Currency: " + curr);


             response = add_expense(email_id, first_name, last_name, category, amt, curr);
           }
           else if(action.equals("delete")){
             System.out.println("Action Delete");
             JSONObject unit_curr = (JSONObject) par.get("unit-currency");
             String curr = (String) unit_curr.get("currency");
            System.out.println("Currency: " + curr);
             Double   amt = (Double) unit_curr.get("amount");
             System.out.println("amount: " + amt);

             amt = amt * -1;
             response = add_expense(email_id, first_name, last_name, category, amt, curr);
           }
           else if(action.equals("spend")){
               System.out.println("Spend action");


               if(!par.get("date").equals("") && par.get("date-period").equals("")){
                 String date = (String) par.get("date");
                 System.out.println("Date:" + par.get("date"));
                 System.out.println(par.get("date").getClass().getName());
                 String fdate = date.substring(0,10);
                 System.out.println(fdate);
                 LocalDate ffdate = LocalDate.parse(fdate);
                 System.out.println(ffdate);
                 response = get_expenseToday(email_id, category, ffdate);
               }
               else if(!par.get("date-period").equals("") && par.get("date").equals("")) {
                 JSONObject dateperiod = (JSONObject) par.get("date-period");

                 System.out.println("dateperiod");
                 String startDate = (String) dateperiod.get("startDate");
                 System.out.println(startDate);
                 String endDate = (String) dateperiod.get("endDate");
                 System.out.println(endDate);
                 String fstartDate = startDate.substring(0,10);
                 System.out.println(fstartDate);
                 String fendDate = endDate.substring(0,10);
                 System.out.println(fendDate);
                 LocalDate ffstartDate = LocalDate.parse(fstartDate);
                 System.out.println(ffstartDate);
                 LocalDate ffendDate = LocalDate.parse(fendDate);
                 System.out.println(ffendDate);
                 response = get_expensePeriod(email_id, category, ffstartDate, ffendDate);
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
      //     responseBody.put("message", "Output is" + 2);
        //   JSONObject headerJson = new JSONObject();
      //     headerJson.put("x-custom-header", "my custom header value");
    //       headerJson.put("Access-Control-Allow-Origin", "*");
      //     responseJson.put("isBase64Encoded", false);
           responseJson.put("statusCode", responseCode);
        //   responseJson.put("headers", headerJson);
           responseJson.put("body", responseBody.toString());
           OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
           writer.write(responseJson.toJSONString());
           writer.close();


   }
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
            if(category == ""){
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
              //  System.out.print("Column 1 returned ");
              res = resultSet.getString(1);
              System.out.println(resultSet.getString(1));
              //  System.out.println(resultSet.getString(3));
              //  System.out.println(resultSet.getString(4));
              //  System.out.println(resultSet.getString(5));
              //  System.out.println(resultSet.getString(6));
              //  System.out.println(resultSet.getString(7));
            }
            resultSet.close();
          //  readStatement.close();
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
       return res;

   }
   public String get_expenseToday(String email_id, String category, LocalDate date){
         System.out.println("Inside get_expenseToday");
         System.out.println(email_id + " " + category+ " " +date);
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
          //   readStatement = conn.createStatement();
            if(category == ""){
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
              //  System.out.print("Column 1 returned ");
                System.out.println(resultSet.getString(1));
                res = resultSet.getString(1);

              //  System.out.println(resultSet.getString(3));
              //  System.out.println(resultSet.getString(4));
              //  System.out.println(resultSet.getString(5));
              //  System.out.println(resultSet.getString(6));
              //  System.out.println(resultSet.getString(7));
            }
            resultSet.close();
          //  readStatement.close();
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
       return res;
   }
   public String add_expense(String email_id, String first_name, String last_name, String category, Double amt, String curr){
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
          //   DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
             LocalDate now = LocalDate.now();
            // System.out.println(dtf.format(localDate)); //2016/11/16
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
       return "Successfully added " + amt + "to " + category;
   }

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
