package cz.brmlab.yodaqa.io.bioasq;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BioASQFileWriter {
	@SuppressWarnings("unchecked")
	public static String writeSubmissionFile(JSONArray results,String name) {

		JSONObject resultObject=new JSONObject();
		resultObject.put("username", "wasim");
		resultObject.put("password", "wasim");
		resultObject.put("system", "TestSystem");
		resultObject.put("questions", results);
		
		//System.out.println(resultObject.toJSONString());

		FileWriter file = null;
		try
		{
			String path ="BioASQ-task3bPhaseB-"+name+".json";
			file=new FileWriter(path);
			file.write(resultObject.toJSONString());
			System.out.println("File Succussfully saved in Resources");
			return path;
		}
		catch(IOException r)
		{
			r.getMessage();
		}
		finally
		{

			try {
				file.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
