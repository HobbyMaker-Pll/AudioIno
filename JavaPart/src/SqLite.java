import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SqLite {
	private Connection con;
	
	/*
	 * Statement state = con.createStatement();
	 * ResultSet res = state.executeQuery("SELECT name, password FROM users");
	 * 
	 * PreparedStatement prep = con.prepareStatement("INSERT INTO users values(?,?,?);");
	 * prep.setInt(1, 1);
   	 * prep.setString(2, "Victor Pillon");
   	 * prep.setString(3, "Aninha");
   	 * prep.execute();
	 * 
	 */
	
	public void ConnectDB() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		con = DriverManager.getConnection("jdbc:sqlite:Music.db");
		con.setAutoCommit(true);
	}
	
	public void DisconnectDB() throws SQLException {
		if (!con.isClosed()) {
			con.close();
		}
	}
	
	public void CreateTable(String name) throws SQLException {
		String statement = "CREATE TABLE " 
				+ name 
				+ "(ID INTEGER PRIMARY KEY NOT NULL, "
				+ "PIN STRING, "
				+ "NAME STRING, "
				+ "PATH STRING )";
		
		Statement state = con.createStatement();
		state.execute(statement);
		
		state.close();
	}
	
	public void DeleteTable(String name) throws SQLException {
		String statement = "DROP TABLE " + name;
		Statement state = con.createStatement();
		state.execute(statement);
		
		state.close();
	}
	
	public void addSong(String table, String Pin, String Name, String Path) throws SQLException {
		String prepStatement = "INSERT INTO "+ table +" VALUES (null, ?, ?, ?)";
		PreparedStatement prep = con.prepareStatement(prepStatement);
		prep.setString(1, Pin);
		prep.setString(2, Name);
		prep.setString(3, Path);
		prep.execute();
		
		prep.close();
	}

	public ArrayList<String> getTableNames() throws SQLException{
		ArrayList<String> names = new ArrayList<String>();
		
		String statement = "SELECT name FROM sqlite_master WHERE type='table'";
		Statement state = con.createStatement();
		ResultSet result = state.executeQuery(statement);
		
		while (result.next()) {
			names.add(result.getString("name"));
		}
		
		state.close();
		result.close();
		
		return names;
	}
	
	public ArrayList<String> getRow (String table, int condition) throws SQLException{
		ArrayList<String> data = new ArrayList<String>();
		
		String prepStatement = "SELECT PIN,NAME FROM " + table + " WHERE ID=?";
		PreparedStatement prep = con.prepareStatement(prepStatement);
		prep.setInt(1, condition);
		ResultSet result = prep.executeQuery();
		
		data.add(result.getString("PIN"));
		data.add(result.getString("NAME"));
		
		prep.close();
		result.close();
				
		return data;
	}
	
	public int getTablesize(String table) throws SQLException {
		String statement = "SELECT COUNT(*) AS count FROM " + table;
		Statement state = con.createStatement();
		ResultSet result = state.executeQuery(statement);
		int count = result.getInt("count");
		
		state.close();
		result.close();
		
		return count;
	}
	
	public String getPath(String Pin, String TableName) throws SQLException {
		String prepStatement = "SELECT PATH FROM " + TableName + " WHERE PIN=?";
		PreparedStatement prep = con.prepareStatement(prepStatement);
		prep.setString(1, Pin);
		ResultSet result = prep.executeQuery();
		
		String Path = result.getString("PATH");
		
		prep.close();
		result.close();

		return Path;
	}
}