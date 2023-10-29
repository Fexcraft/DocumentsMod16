package net.fexcraft.app.json;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * 
 * Fex's Json Lib
 * 
 * @author Ferdinand Calo' (FEX___96)
 *
 */
public class JsonHandler {
	
	private static String NUMBER = "^\\-?\\d+$";
	private static String FLOATN = "^\\-?\\d+\\.\\d+$";
	
	public static JsonObject<?> parse(String str, boolean defmap){
		return parse(new ByteArrayInputStream(str.getBytes()), defmap);
	}
	
	public static JsonObject<?> parse(InputStream stream, boolean defmap){
		JsonObject<?> root = null;
		Parser parser = new Parser();
		//long time = Time.getDate();
		ISW isw = new ISW(stream);
		try{
			isw.next();
			if(isw.starts('{')){
				root = parser.parseMap(new JsonMap(), isw);
			}
			else if(isw.starts('[')){
				root = parser.parseArray(new JsonArray(), isw);
			}
			else root = defmap ? new JsonMap() : new JsonArray();
		}
		catch(Exception e){
			e.printStackTrace();
			return defmap ? new JsonMap() : new JsonArray();
		}
		//Print.console("Time taken: " + (Time.getDate() - time));
		return root;
	}

	public static JsonObject<?> parse(File file, boolean defmap){
		try{
			return parse(new FileInputStream(file), defmap);
		}
		catch(IOException e){
			e.printStackTrace();
			return defmap ? new JsonMap() : new JsonArray();
		}
	}

	public static JsonMap parse(File file){
		return parse(file, true).asMap();
	}

	public static JsonMap parse(InputStream stream) throws IOException {
		return parse(stream, true).asMap();
	}
	
	private static class Parser {

		private JsonObject<?> parseMap(JsonMap root, ISW isw) throws IOException {
			if(isw.starts('{')) isw.next();
			while(isw.has()){
				isw.skip();
				if(isw.cher == '"'){
					String key = isw.till('"');
					isw.skip();
					isw.next();//skipping colon
					isw.skip();
					if(isw.cher == '{'){
						root.add(key, parseMap(new JsonMap(), isw));
					}
					else if(isw.cher == '['){
						root.add(key, parseArray(new JsonArray(), isw));
					}
					else if(isw.cher == '"'){
						root.add(key, parseValue(isw.till('"')));
					}
					else{
						root.add(key, parseValue(isw.till()));
					}
				}
				else if(isw.cher == ',') isw.next();
				else if(isw.cher == '}') break;
				else isw.next();
			}
			if(isw.starts('}')) isw.next();
			return root;
		}

		private JsonObject<?>  parseArray(JsonArray root, ISW isw) throws IOException {
			if(isw.starts('[')) isw.next();
			while(isw.has()){
				isw.skip();
				if(isw.cher == '"'){
					root.add(parseValue(isw.till('"')));
				}
				else if(isw.cher == '{'){
					root.add(parseMap(new JsonMap(), isw));
				}
				else if(isw.cher == '['){
					root.add(parseArray(new JsonArray(), isw));
				}
				else if(isw.cher == ',') isw.next();
				else if(isw.cher == ']') break;
				else {
					root.add(parseValue(isw.till()));
				}
			}
			if(isw.starts(']')) isw.next();
			return root;
		}

		private static JsonObject<?> parseValue(String val){
			val = val.trim();
			if(val.equals("null")){
				return new JsonObject<String>(val);//new JsonObject<Object>(null);
			}
			else if(Pattern.matches(NUMBER, val)){
				long leng = Long.parseLong(val);
				if(leng < Integer.MAX_VALUE){
					return new JsonObject<>((int)leng);
				}
				else return new JsonObject<>(leng);
			}
			else if(Pattern.matches(FLOATN, val)){
				return new JsonObject<>(Float.parseFloat(val));
			}
			else if(val.equals("true")) return new JsonObject<>(true);
			else if(val.equals("false")) return new JsonObject<>(false);
			else return new JsonObject<>(val);
		}
		
	}
	
	public static class ISW {

		protected InputStreamReader stream;
		protected char cher;
		protected boolean has = true;

		public ISW(InputStream stream){
			this.stream = new InputStreamReader(stream, StandardCharsets.UTF_8);
		}

		public boolean starts(char c) throws IOException {
			return cher == c;
		}

		private void next() throws IOException {
			int i = stream.read();
			if(i < 0){
				has = false;
				return;
			}
			cher = (char)i;
		}

		public boolean has(){
			return has;
		}

		public void skip() throws IOException {
			if(has && (cher <= ' ' || cher == '\n' || cher == '\r')){
				next();
				skip();
			}
		}

		public String till(char c) throws IOException {
			StringBuffer buffer = new StringBuffer();
			if(cher == '"') next();
			while(has && cher != c){
				buffer.append(cher);
				next();
			}
			next();
			return buffer.toString();
		}

		public String till() throws IOException {
			StringBuffer buffer = new StringBuffer();
			while(has && cher != ',' && cher != '}' && cher != ']'){
				buffer.append(cher);
				next();
			}
			return buffer.toString();
		}
	}

	public static String toString(JsonObject<?> obj){
		return toString(obj, 0, false, PrintOption.DEFAULT);
	}

	public static String toString(JsonObject<?> obj, PrintOption opt){
		return toString(obj, 0, false, opt);
	}

	public static String toString(JsonObject<?> obj, int depth, boolean append, PrintOption opt){
		String ret = "", tab = "", tabo = "    ", space = opt.spaced ? " " : "", colspace = !opt.flat || opt.spaced ? " " : "";
		String app = append ? "," + space : "", n = opt.flat ? "" : "\n";
		if(!opt.flat){
			for(int j = 0; j < depth; j++){
				tab += tabo;
			}
		}
		else tabo = "";
		if(obj == null){
			ret += "[ \"null\" ]";
		}
		else if(obj.isMap()){
			if(obj.asMap().empty()){
				ret += "{}" + app + n;
			}
			else{
				ret += "{" + space + n;
				Iterator<Entry<String, JsonObject<?>>> it = obj.asMap().value.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, JsonObject<?>> entry = it.next();
					ret += tab + tabo + '"' + entry.getKey() + '"' + ":" + colspace + toString(entry.getValue(), depth + 1, it.hasNext(), opt);
				}
				ret += tab + space + "}" + app + n;
			}
		}
		else if(obj.isArray()){
			if(obj.asArray().empty()){
				ret += "[]" + app + n;
			}
			else{
				ret += "[" + space + n;
				Iterator<JsonObject<?>> it = obj.asArray().value.iterator();
				while(it.hasNext()){
					ret += tab + tabo + toString(it.next(), depth + 1, it.hasNext(), opt);
				}
				ret += tab + space + "]" + app + n;
			}
		}
		else{
			ret += (obj.value instanceof String ? '"' + obj.value.toString() + '"' : obj.value) + app + n;
		}
		return ret;
	}
	
	public static class PrintOption {
		
		public static final PrintOption FLAT = new PrintOption().flat(true).spaced(false);
		public static final PrintOption SPACED = new PrintOption().flat(false).spaced(true);
		public static final PrintOption DEFAULT = SPACED;
		
		boolean flat, spaced;
		
		public PrintOption(){}
		
		public PrintOption flat(boolean bool){
			flat = bool;
			return this;
		}
		
		public PrintOption spaced(boolean bool){
			spaced = bool;
			return this;
		}
		
	}

	public static void print(File file, JsonObject<?> obj, PrintOption opt){
		try{
			Files.write(file.toPath(), toString(obj, opt).getBytes(StandardCharsets.UTF_8));
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	public static JsonMap parseURL(String... adr){
		try{
			URL url = new URL(adr[0]);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod(adr.length > 1 ? "POST" : "GET");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			connection.setConnectTimeout(10000);
			connection.setDoOutput(adr.length > 1);
			if(adr.length > 1){
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(adr[1]);
				wr.flush();
				wr.close();
			}
			//
			JsonMap obj = parse(connection.getInputStream());
			connection.disconnect();
			return obj;
		}
		catch(IOException e){
			e.printStackTrace();
			return new JsonMap();
		}
	}

	public static JsonMap wrap(Map<String, Object> map, JsonMap json){
		if(json == null) json = new JsonMap();
		for(Entry<String, Object> entry : map.entrySet()){
			if(entry.getValue() instanceof Collection){
				json.add(entry.getKey(), wrap((Collection<?>)entry.getValue(), null));
			}
			else if(entry.getValue() instanceof Map){
				json.add(entry.getKey(), wrap((Map<String, Object>)entry.getValue(), null));
			}
			else if(entry.getValue() instanceof String){
				json.add(entry.getKey(), entry.getValue() + "");
			}
			else json.add(entry.getKey(), Parser.parseValue(entry.getValue() + ""));
		}
		return json;
	}

	public static JsonArray wrap(Collection<?> collection, JsonArray json){
		if(json == null) json = new JsonArray();
		for(Object obj : collection){
			if(obj instanceof Collection){
				json.add(wrap((Collection<?>)obj, null));
			}
			else if(obj instanceof Map){
				json.add(wrap((Map<String, Object>)obj, null));
			}
			else if(obj instanceof String){
				json.add(obj + "");
			}
			else json.add(Parser.parseValue(obj + ""));
		}
		return json;
	}

	public static Object dewrap(String obj){
		return dewrap(parse(obj, true).asMap());
	}

	public static HashMap<String, Object> dewrap(JsonMap map){
		HashMap<String, Object> hashmap = new HashMap<>();
		for(Entry<String, JsonObject<?>> entry : map.entries()){
			if(entry.getValue().isMap()){
				hashmap.put(entry.getKey(), dewrap(entry.getValue().asMap()));
			}
			else if(entry.getValue().isArray()){
				hashmap.put(entry.getKey(), dewrap(entry.getValue().asArray()));
			}
			else{
				hashmap.put(entry.getKey(), entry.getValue().value);
			}
		}
		return hashmap;
	}

	public static ArrayList<Object> dewrap(JsonArray array){
		ArrayList<Object> list = new ArrayList<>();
		for(JsonObject<?> obj : array.value){
			if(obj.isMap()){
				list.add(dewrap(obj.asMap()));
			}
			else if(obj.isArray()){
				list.add(dewrap(obj.asArray()));
			}
			else{
				list.add(obj.value);
			}
		}
		return list;
	}

	public static <T> ArrayList<T> dewrapc(JsonArray array){
		ArrayList<Object> list = new ArrayList<>();
		for(JsonObject<?> obj : array.value){
			if(obj.isMap()){
				list.add(dewrap(obj.asMap()));
			}
			else if(obj.isArray()){
				list.add(dewrap(obj.asArray()));
			}
			else{
				list.add(obj.value);
			}
		}
		return (ArrayList<T>)list;
	}

}
