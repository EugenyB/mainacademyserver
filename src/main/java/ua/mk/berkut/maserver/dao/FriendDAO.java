package ua.mk.berkut.maserver.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class FriendDAO {
    private Connection connection;

    public FriendDAO(Connection connection) {
        this.connection = connection;
    }


    public Set<Integer> getFriendsFor(int id) {
        Set<Integer> result = new HashSet<>();
        try (PreparedStatement ps1 = connection.prepareStatement("select id2 from friend where id1 = ?");
             PreparedStatement ps2 = connection.prepareStatement("select id1 from friend where id2 = ?")
        ) {
            ps1.setInt(1, id);
            ps2.setInt(1, id);
            ResultSet rs = ps1.executeQuery();
            while (rs.next()) {
                int val = rs.getInt(1);
                result.add(val);
            }
            Set<Integer> result2 = new HashSet<>();
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                Integer val = rs2.getInt(1);
                result2.add(val);
            }
            result.retainAll(result2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void addFriendFor(int from, int to) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into friend (id1, id2) values (?, ?)")) {
            preparedStatement.setInt(1, from);
            preparedStatement.setInt(2, to);
            int count = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
