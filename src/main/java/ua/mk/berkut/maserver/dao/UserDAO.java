package ua.mk.berkut.maserver.dao;

import ua.mk.berkut.maserver.db.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    public void addUser(User user) {
        try (PreparedStatement ps = connection.prepareStatement("insert into chatuser (login, password, username, birthday, city, description) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getUsername());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            ps.setString(5, user.getCity());
            ps.setString(6, user.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
        List<User> result = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("select * from chatuser");
            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                String password = rs.getString("password");
                String username = rs.getString("username");
                String city = rs.getString("city");
                String description = rs.getString("description");
                Date date = rs.getDate("birthday");
                LocalDate birthday;
                if (date!=null) {
                    birthday = date.toLocalDate();
                } else {
                    birthday = LocalDate.of(1900, 1, 1);
                }
                result.add(new User(id,login,password,username,birthday,city,description));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
