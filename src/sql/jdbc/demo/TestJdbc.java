package sql.jdbc.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 重新复习下jdbc
 * @author Yuer
 *
 */
public class TestJdbc {
	
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		
		// 先加载
		Class.forName("com.mysql.jdbc.Driver");
		
		// 获取连接 注意导入的是java.sql包的
		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mybatis","root","404221");
		
		String sql = "select * from tb_user";
		
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		
		while(rs.next()) {
			System.out.println("id:" +  rs.getInt("id") + "\t" + "userName:" + rs.getString("username") + "\t" + "address:" + rs.getString("address"));
			
		}
		
		
	}

}
