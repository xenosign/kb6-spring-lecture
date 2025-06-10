//package org.example.kb6spring.config.member2;
//
//import org.example.kb6spring.domain.member.MemberVO;
//import org.springframework.stereotype.Repository;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//@Repository
//public class MemberRepositoryV1 {
//
//    private static final String URL = "jdbc:mysql://localhost:3306/member?useUnicode=true&characterEncoding=utf8";
//    private static final String USER = "root";
//    private static final String PASSWORD = "1234";
//
//    public List<MemberVO> findAll() {
//        List<MemberVO> members = new ArrayList<>();
//
//        Connection connection = null;
//        PreparedStatement preparedStatement = null;
//        ResultSet resultSet = null;
//
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//            connection = DriverManager.getConnection(URL, USER, PASSWORD);
//
//            String sql = "SELECT id, email, name, grade, asset FROM member";
//            preparedStatement = connection.prepareStatement(sql);
//            resultSet = preparedStatement.executeQuery();
//
//            while (resultSet.next()) {
//                MemberVO member = new MemberVO();
//                member.setId(resultSet.getLong("id"));
//                member.setEmail(resultSet.getString("email"));
//                member.setName(resultSet.getString("name"));
//                member.setGrade(resultSet.getString("grade"));
//                member.setAsset(resultSet.getLong("asset"));
//                members.add(member);
//            }
//        } catch (ClassNotFoundException e) {
//            System.out.println("MySQL JDBC 드라이버를 찾을 수 없습니다.");
//            e.printStackTrace();
//        } catch (SQLException e) {
//            System.out.println("DB 연결 또는 조회 중 오류 발생");
//            e.printStackTrace();
//        } finally {
//            try {
//                if (resultSet != null) resultSet.close();
//                if (preparedStatement != null) preparedStatement.close();
//                if (connection != null) connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return members;
//    }
//}
