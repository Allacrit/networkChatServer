package server.services;

public interface AuthenticationService {
    String getUsernameByLoginAndPassword(String login, String password);
    String checkCreateNewUser(String[] userData);
}



