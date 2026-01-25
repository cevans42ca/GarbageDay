package ca.quines.garbageday;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class GarbageDayServer {

	private static final String SAVE_FILENAME = ".garbageDay";

	private HttpServer httpServer;
	private GarbageDayManager garbageDayManager;

	/**
	 * Define the specific type of Map that we'll be processing to resolve the complier warnings.
	 * The Type object created via TypeToken is a java.lang.reflect.Type. In Java, these
	 * reflection-based type representations are immutable metadata objects provided by the
	 * JVM or the library. Since they have no internal state that changes during execution,
	 * they are inherently thread-safe to read and share.
	 */
    private static final java.lang.reflect.Type MAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();

	public static void main(String[] args) {
		try {
			GarbageDayServer garbageDayServer;
			if (args.length == 0) {
				garbageDayServer = new GarbageDayServer(List.of("192.168", "10.0"));
			}
			else {
				garbageDayServer = new GarbageDayServer(List.of(args));
			}

			garbageDayServer.addShutdownListener();
			garbageDayServer.httpServer.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Allow a shutdown in Eclipse with "shutdown" at stdin.  Ctrl+C works at the console, but
	 * not in Eclipse.
	 */
	private void addShutdownListener() {
		Thread consoleListener = new Thread(() -> {
		    System.out.println("Console listener started. Type 'shutdown' to stop the server.");
		    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
		        String line;
		        while ((line = reader.readLine()) != null) {
		            if ("shutdown".equalsIgnoreCase(line.trim())) {
		                System.out.println("Shutdown command received. Stopping server...");
		    		    httpServer.stop(5);
		                break;
		            }
		        }
		    }
		    catch (IOException e) {
		        System.err.println("Error reading from console: " + e.getMessage());
		    }
		});

		consoleListener.setDaemon(false); // Ensure this thread keeps the JVM alive.
		consoleListener.start();
	}

	public String getAddressFullDisplay(NetworkInterface netInterface, InetAddress address) {
		return netInterface.getName() + " / " + netInterface.getDisplayName() + " / " + address.getHostAddress();
	}

	public GarbageDayServer(List<String> interfaceSpecList) throws UnknownHostException, IOException, IllegalArgumentException {
		List<InetAddress> foundInterfaceList = new ArrayList<>();
		InetAddress foundInterface = null;
		Enumeration<NetworkInterface> interfaceEnum = NetworkInterface.getNetworkInterfaces();
		while(interfaceEnum.hasMoreElements()) {
			NetworkInterface netInterface = (NetworkInterface) interfaceEnum.nextElement();
			Enumeration<InetAddress> addressesEnum = netInterface.getInetAddresses();
			while(addressesEnum.hasMoreElements())
			{
				InetAddress address = (InetAddress) addressesEnum.nextElement();
				String addressFullDisplay = getAddressFullDisplay(netInterface, address);
				System.out.println(addressFullDisplay);

				for (String interfaceSpec : interfaceSpecList) {
					if (addressFullDisplay.indexOf(interfaceSpec) > -1) {
						foundInterfaceList.add(address);
					}
				}
			}
		}

		if (foundInterfaceList.size() == 0) {
			for (String interfaceSpec : interfaceSpecList) {
				System.err.println("No match for interface " + interfaceSpec);
			}

			throw new IllegalArgumentException();
		}
		else if (foundInterfaceList.size() == 1) {
			foundInterface = foundInterfaceList.get(0);
		}
		else if (foundInterfaceList.size() > 1) {
			System.err.println();
			System.err.println("Too many matches for interface " + interfaceSpecList + ":");
			for (InetAddress interfaceEntry : foundInterfaceList) {
				System.out.println(interfaceEntry.getHostAddress());
			}
			throw new IllegalArgumentException();
		}

		System.out.println("Listening for connections to:  " + foundInterface + ".");

		File configFile = new File(System.getProperty("user.home"), SAVE_FILENAME);
		System.out.println("Using config file '" + configFile.getAbsolutePath() + "'.");

		garbageDayManager = new GarbageDayManager(configFile, null);

		InetSocketAddress inetSocketAddress = new InetSocketAddress(foundInterface, 8081);
		this.httpServer = HttpServer.create(inetSocketAddress, 10);
		this.httpServer.createContext("/", (he) -> rootContextHandler(he));
		this.httpServer.createContext("/saveGarbageDay", (he) -> saveDayContextHandler(he));
		this.httpServer.createContext("/getGarbageDay", (he) -> getDayContextHandler(he));
	}

	private void sendResponseHeadersOK(HttpExchange he) throws IOException {
		he.sendResponseHeaders(200, 0);
		he.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
	}

	private void sendText(HttpExchange he, String text) throws IOException {
		OutputStream os = he.getResponseBody();
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {
			bw.write(text);
		}
	}

	private void rootContextHandler(HttpExchange he) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(he.getRequestBody()))) {
			System.out.println(br.readLine());
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		try {
			sendResponseHeadersOK(he);
			sendText(he, "Ready");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendErrorResponse(HttpExchange he, int errorCode, String errorMessage)
			throws IOException
	{
		he.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
		he.sendResponseHeaders(errorCode, 0);

		OutputStream os = he.getResponseBody();
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {
			sendText(he, "Error.  Check the logs.");
		}
	}

	private Map<String, String> handlePost(HttpExchange he) throws IOException {
		String requestMethod = he.getRequestMethod();
		if (!"POST".equals(requestMethod)) {
			System.out.println("Request method " + requestMethod + " is not allowed for updating.");
			sendErrorResponse(he, 400, "Only POST is allowed for updating.");

			return null;
		}

		URI requestUri = he.getRequestURI();
		System.out.println(requestUri);

		InputStream is = he.getRequestBody();
		Map<String, String> queryMap = null;
		try {
		    Gson gson = new Gson();
			queryMap = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), MAP_TYPE);
		}
		catch (JsonSyntaxException | JsonIOException e) {
			e.printStackTrace();
			sendErrorResponse(he, 500, "Internal Error.  Check the logs on the host.");

			return null;
		}

		return queryMap;
	}

	private void saveDayContextHandler(HttpExchange he) {
		try {
			Map<String, String> requestParams = handlePost(he);
			System.out.println(requestParams);

			String response = garbageDayManager.addDay(requestParams.get("date"));

			sendResponseHeadersOK(he);
			sendText(he, response);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getDayContextHandler(HttpExchange he) {
		try {
			String response = garbageDayManager.getNextGarbageDay();

			sendResponseHeadersOK(he);
			sendText(he, response);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
