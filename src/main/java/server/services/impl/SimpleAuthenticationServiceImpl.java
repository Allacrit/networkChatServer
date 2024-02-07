package server.services.impl;

import server.models.User;
import server.services.AuthenticationService;
import java.util.ArrayList;
import java.util.List;


public class SimpleAuthenticationServiceImpl implements AuthenticationService {

    private static List<User> clients = new ArrayList<User>() {{
          add(new User("martin","1","Мартин_Макфлай"));
          add(new User("batman","1","Брюс_Уэйн"));
          add(new User("gena","1","Гендальф_Серый"));
          add(new User("bender","1","Бендер_Родригес"));
    }};

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User client : clients) {
            if (client.getLogin().equals(login) && client.getPassword().equals(password)) {
                return client.getUsername();
            }
        }
        return null;
    }

    public String checkCreateNewUser(String[] userData) {
        String login = null;
        String name = null;
        for (User client : clients) {

            if (client.getLogin().equals(userData[1])) {
                login = "login";
            }

            if (client.getUsername().equals(userData[3])) {
                name = "name";
            }
        }

        if (("login").equals(login)) {
            return login;

        }else if (("name").equals(name)) {
            return name;

        } else {
            createNewUser(userData);
            return null;
        }
    }
    private static void createNewUser(String[] userData) {
        clients.add(new User(userData[1], userData[2], userData[3]));
        System.out.printf("Создан новый пользователь: %s%n",userData[3]);
    }
}
