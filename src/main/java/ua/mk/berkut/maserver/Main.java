package ua.mk.berkut.maserver;

import ua.mk.berkut.maserver.clients.ClientThread;
import ua.mk.berkut.maserver.dao.FriendDAO;
import ua.mk.berkut.maserver.dao.UserDAO;
import ua.mk.berkut.maserver.db.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

    /**
     * Разделитель слов в строках
     */
    private final static String SEPARATOR = ";";

    private Properties properties = new Properties();


    /**
     * Список всех пользователей
     */
    private List<User> users;

    /**
     * Список потоков, обслуживающих пользователей
     */
    private List<ClientThread> onlineUserThreads;

    // Объекты доступа к данным
    private UserDAO userDAO;
    private FriendDAO friendDAO;

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    /**
     * Основной метод работы сервера
     */
    private void run() throws Exception {
        // Подключение к БД - не закрывается, пока работает сервер
        try (Connection ignored = startServer()) {
            users = userDAO.getAllUsers();
            onlineUserThreads = new ArrayList<>();
            printUsers(users);
            ServerSocket serverSocket = new ServerSocket(
                    Integer.parseInt(properties.getProperty("port", "1234"))
            );
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                Socket socket = serverSocket.accept();
                new ClientThread(socket, this).start();
            }
        }
        // connection.close(); // - закрыть подключение, если реализован выход
    }

    // отладочный метод - печать всех пользователей
    private void printUsers(List<User> users) {
        users.forEach(System.out::println);
    }

    /**
     * Запуск сервера
     * @return подключение к БД
     * @throws IOException если сетевое подключение невозможно
     * @throws SQLException если произошла ошибка с БД
     */
    private Connection startServer() throws IOException, SQLException {

        properties.load(Files.newBufferedReader(Paths.get("chat.cfg")));
        Connection connection = DriverManager.getConnection(
                properties.getProperty("url"),
                properties);
        userDAO = new UserDAO(connection);
        friendDAO = new FriendDAO(connection);
        return connection;
    }

    /**
     * Отключение клиента, завершившего работу
     * @param clientThread ссылка на поток клиента, завершившего работу
     */
    public void remove(ClientThread clientThread) {
        onlineUserThreads.remove(clientThread);
    }

    /**
     * Поиск пользователя по логину и паролю. Для проверки, был ли ранее зарегистрирован пользователь с такими данными
     * @param login введенный логин пользователя
     * @param password введенный пароль пользователя
     * @return объект пользователя, включая список ID друзей или null если пользователь с такими login-password не зарегистрирован
     */
    public User findUser(String login, String password) {
        User user = userDAO.findUser(login, password);
        if (user != null) {
            user.setFriendsIds(friendDAO.getFriendsFor(user.getId()));
        }
        return user;
    }

    /**
     * Список пользователей online
     * @return список всех пользователей, которіе сейчас online
     */
    public List<User> getOnlineUsers() {
        return onlineUserThreads
                .stream()
                .map(ClientThread::getUser)
                .collect(Collectors.toList());
    }

    /**
     * Пересылка сообщения от одного клиента другому
     * @param message сообщение, передаваемое от одного клиента другому
     */
    public synchronized void processMessage(String message) {
        String[] split = message.split(SEPARATOR);
        if (split.length!=3) return;
        String receiver = split[0];
        String sender = split[1];
        String text = split[2];
        // Среди всех потоков пользователей, выбрать те, чей логин соответствует переданному
        Optional<ClientThread> clientThread = onlineUserThreads
                .stream()
                .filter(t -> t.getUser().getLogin().equals(receiver))
                .findFirst(); // так как не может быть более одного такого логина
        clientThread.ifPresent(thread -> thread.send(sender, text));
    }

    /**
     * Добавляет поток клиента к списку online
     * @param clientThread поток, обслуживающий клиента
     */
    public synchronized void addToOnline(ClientThread clientThread) {
        onlineUserThreads.add(clientThread);
    }

    /**
     * Регистрация нового пользователя
     * @param line строка содержащая данные регистрации. Формат строки: register;login;password;username;date;city
     * @return нового зарегистрированного пользователя или null, если регистрация не удалась
     */
    public User register(String line) {
        try {
            String[] s = line.split(SEPARATOR);
            if (s.length!=6) return null;
            String login = s[1];
            String password = s[2];
            String username = s[3];
            String dateStr = s[4];
            String[] split = dateStr.split("\\D");
            LocalDate birthday = LocalDate.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
            String city = s[5];
            User user = new User(login, password, username, birthday, city, "");
            user = userDAO.addUser(user);
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Реакция на сообщение о добавлении друга
     * @param line информация о добавлении друга в формате: кто_добавляет;кого_добавляет
     */
    public void processAddFriend(String line) {
        String[] s = line.split(SEPARATOR);
        if(s.length!=2) return;
        String login1 = s[0];
        String login2 = s[1];
        User u1 = userDAO.findByLogin(login1);
        User u2 = userDAO.findByLogin(login2);
        friendDAO.addFriendFor(u1.getId(), u2.getId());
    }

    @SuppressWarnings("unused")
    public List<User> getUsers() {
        return users;
    }
}
