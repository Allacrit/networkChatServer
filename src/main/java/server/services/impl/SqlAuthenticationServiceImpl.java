package server.services.impl;

import java.sql.*;

import org.apache.log4j.Logger;
import server.services.AuthenticationService;



public class SqlAuthenticationServiceImpl implements AuthenticationService {

    private static Connection connection;
    private static Statement stmt;
    private final Logger fileLogSystem = Logger.getLogger("file");
    private final Logger consoleLog = Logger.getLogger("console");


    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        try {
            connection();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * from auth WHERE login = '%s'",login));
            if (rs.isClosed()) {
                return null;
            }
            String usernameDB = rs.getString("username");
            String passwordDB = rs.getString("password");
            return ((passwordDB != null) && (passwordDB.equals(password))) ? usernameDB : null;
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
            return null;
        }finally {
            disconnection();
        }
    }

    public String checkCreateNewUser(String[] newUserData) {
        connection();

        String loginNewUser = newUserData[1];
        String usernameNewUser = newUserData[3];
        String resultLogin = null;
        String resultName = null;

        try {
            ResultSet result = stmt.executeQuery(String.format("SELECT * from auth WHERE login = '%s' OR username = '%s'",loginNewUser,usernameNewUser));
            if (result.isClosed()) {
                createNewUser(newUserData);
            }else {
                resultLogin = result.getString("login");
                resultName = result.getString("username");
            }
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        } finally {
            disconnection();
        }
        if (loginNewUser.equals(resultLogin)) {
            return "login";
        } else if (usernameNewUser.equals(resultName)) {
            return "name";
        } else {
            return null;
        }
    }

    private void createNewUser(String[] newUserData) {
        try {
            connection.setAutoCommit(false);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO auth (login, password, username) VALUES (?,?,?)");
            preparedStatement.setString(1,newUserData[1]);
            preparedStatement.setString(2,newUserData[2]);
            preparedStatement.setString(3,newUserData[3]);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        }
        consoleLog.info(String.format("Создан новый пользователь: %s",newUserData[3]));
    }

    private synchronized void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/db/mainDB");
            stmt = connection.createStatement();
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        }

    }

    private synchronized void disconnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        }
    }

    public void updateUsernameByLogin(String login, String newUsername) {
        try {
            stmt.executeUpdate(String.format("UPDATE auth SET username = '%s' WHERE login = '%s'",newUsername,login));
        } catch (SQLException e) {
            fileLogSystem.error(e.getMessage());
            consoleLog.error(e.getMessage());
        }
    }
}
