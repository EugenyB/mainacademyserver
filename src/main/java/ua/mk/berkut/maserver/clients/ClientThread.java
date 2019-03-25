package ua.mk.berkut.maserver.clients;

import ua.mk.berkut.maserver.Main;
import ua.mk.berkut.maserver.db.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Поток, взаимодействующий с коиентом. Для каждого клиента создается свой
 */
public class ClientThread extends Thread {
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private Main main;
    private User user;

    /**
     * Конструктор потока
     * @param socket сокет для подключения
     * @param main ссылка на объект главного класса сервера
     */
    public ClientThread(Socket socket, Main main) {
        this.socket = socket;
        this.main = main;
    }

    /**
     * Получение пользователя, ассоциированного с потоком
     * @return пользователя, ассоциированного с потоком
     */
    public User getUser() {
        return user;
    }

    /**
     * Главный метод потока, в нем происходит "общение" клиента с сервером
     */
    @Override
    public void run() {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            this.in = in; this.out = out;
            if (login()){
                main.addToOnline(this);
                List<User> onlineUsers;// = main.getOnlineUsers();
                String message;
                while ((message = in.readLine())!=null) {
                    if ("<<<".equals(message)) { // Show online friends
                        onlineUsers = main.getOnlineUsers();
                        sendList(onlineUsers.stream().filter(u->user.getFriendsIds().contains(u.getId())).collect(Collectors.toList()));
                    } else if (message.startsWith("+++")) {
                        main.processAddFriend(message.substring(3));
                    } else if (">>>exit<<<".equals(message)) {
                        break;
                    } else {
                        main.processMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            main.remove(this);
        }
    }

    /**
     * Отправка списка клиентов подключенному пользователю
     * @param list список, содержащий информацию о пользователях в формате: id;username;login
     */
    private void sendList(List<User> list) {
        out.println("<<<");
        for (User u : list) {
            out.println(u.getId()+";"+u.getUsername()+";"+u.getLogin());
        }
        out.println("<<<");
    }

    /**
     * Обработка логина и пароля вновь подключившегося пользователя.
     * Сначала отправляется признак подключения: {@code Server Ok}
     * Затем, проверяются login-password и если подключение удалось, отправлем признак подключения: {@code Login Ok}
     * @return true, если подключение успешно, и false - в противном случае
     * @throws IOException если произошла ошибка при подключении
     */
    private boolean login() throws IOException {
        out.println("Server Ok");
        String line = in.readLine();
        if (line.startsWith("register")) {
            return register(line);
        }
        String[] s = line.split(";");
        // s[0] - "login"
        // s[1] === login
        // s[2] === password
        if (s.length!=3) {
            out.println("Login failed 1");
            return false;
        }
        if (!"login".equals(s[0])) {
            out.println("Login failed 2");
            return false;
        }
        String login = s[1];
        String password = s[2];
        User user = findUser(login, password);
        if (user==null) {
            out.println("Login failed 3");
            return false;
        }
        this.user = user;
        out.println("Login Ok");
        return true;
    }

    /**
     * Метод, делегирующий в Main поиск пользователя по логину и паролю
     * @param login логин пользователя
     * @param password пароль пользователя
     * @return найденного пользователя
     */
    @SuppressWarnings("WeakerAccess")
    public User findUser(String login, String password) {
        return main.findUser(login, password);
    }

    /**
     * Метод, делегирующий в Main регистрацию пользователя
     * @param line строка регистрации
     * @return true - если пользователь зарегистрировался успешно, false - в противном случае
     */
    private boolean register(String line) {
        User user = main.register(line);
        if (user==null) return false;
        this.user = user;
        return true;
    }

    /**
     * Отправка сообщения клиенту, ассоциированному с этим потоком
     * @param sender от кого
     * @param text текст сообщения
     */
    public void send(String sender, String text) {
        out.println(">>>"+sender+">>>"+text);
    }
}
