package BrowserInteraction;

import java.io.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import Network.Network;
import Utilities.*;

public class BrowserRequestHandler implements HttpHandler {
	
	public void handle(HttpExchange t) throws IOException {
		try {
			String url , method, data, host;
			
			url = t.getRequestURI().toString();
			method= t.getRequestMethod();
			host = t.getRequestURI().getHost();
			data = Utilities.convertStreamToString(t.getRequestBody()); 
			data=data.replaceAll("\t", "");
			data=data.replaceAll("\r", "");
			data=data.replaceAll("\n", "");
			data=data.replaceAll("\f", "");
			data=data.replaceAll("\0", "");
			data=data.replaceAll(" ", "");
			if (url.indexOf(host+"/db")!=-1) 
			{
				method = "DB" ;
			}
			if (url.indexOf(host+"/save")!=-1)
			{
				method = "SAVE" ;
			}
			if (url.indexOf(host+"/share")!=-1)
			{
				method = "SHARE" ;
			}
			if (url.indexOf(host+"/getshared")!=-1)
			{
				method = "GETSHARED" ;
			}
			if (url.indexOf(host+"/getsaved")!=-1)
			{
				method = "GETSAVED" ;
			}
			try{
			System.out.println("\n" + url + " " + method + " " + data + " " + host );
			}
			catch (Exception e){
				
			}
			
			Response response=null;
			switch (method)
			{
				case "POST":
					response = Network.post(url,host,data);
					break;
				case "GET":
					response = Network.get(url,host);
					break;
				case "DB":
					response = Network.database(url,host,data);
					break;
				case "SAVE":
					response = Network.save(url,host,data);
					break;
				case "SHARE":
					response = Network.share(url,host,data);
					break;
				case "GETSHARED":
					response = Network.getshared(url,host,data);
					break;
				case "GETSAVED":
					response = Network.getsaved(url,host,data);
					break;
				default:
					throw new Exception("ERROR! Method is not classified");
			}
			if (response.getHeaderAttributes().length%2==1) throw new Exception("ERROR! Header attributes are not passed correctly!");
			
			for (int i=0;i<response.getHeaderAttributes().length;i+=2)
			{
				t.getResponseHeaders().add(response.getHeaderAttributes()[i],response.getHeaderAttributes()[i+1]);
			}
			
	        t.sendResponseHeaders(response.getStatusCode(), Integer.toUnsignedLong(response.getResponse().length));
	        OutputStream out = t.getResponseBody();
	        out.write(response.getResponse());
	        out.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    }
	
	/*public static void main(String[] args) throws Exception {
        server = HttpServer.create(new InetSocketAddress(Constants.listenPort), 0);
        server.createContext("/", new BrowserRequestHandler());
        server.start();
    }
	
	/*public static void main(String[] argv)
	{
		BrowserRequestHandler b=null;
		try {
			b = new BrowserRequestHandler();
		} catch (IOException e) {
			e.printStackTrace();
		}
		b.start();
	}*/
	
	
	
}



