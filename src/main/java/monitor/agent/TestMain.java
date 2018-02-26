package monitor.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TestMain {

	public static void main(String[] args) throws IOException {
		System.out.println("SELECT * FROM users WHERE id = ?".replaceFirst("[?]", "123"));
		// TODO Auto-generated method stub
		File file= new File("E:/a.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String temp=null;
		int line = 1;  
        // 一次读入一行，直到读入null为文件结束  
        while ((temp = reader.readLine()) != null) {  
            // 显示行号  
            System.out.print( temp.trim());  
            line++;  
        }  
        reader.close();
	
	
	}

}
