package net.fexcraft.mod.documents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.texture.DownloadingTexture;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;

public class ExternalTextures {

	private static final Map<String, ResourceLocation> MAP = new HashMap<String, ResourceLocation>();
	private static final HashSet<String> KEY = new HashSet<>();
	private static boolean added;
	static{ KEY.add("documents"); }

	public static ResourceLocation get(String url){
		if(MAP.containsKey(url)) return MAP.get(url);
		ResourceLocation texture = new ResourceLocation("documents", url.replaceAll("[^a-z0-9_.-]", ""));
		MAP.put(url, texture);
		if(!added){
			((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).add(new IResourcePack(){
				@Override
				public InputStream getRootResource(String pFileName) throws IOException {
					return null;
				}

				@Override
				public InputStream getResource(ResourcePackType pType, ResourceLocation pLocation) throws IOException{
					return new FileInputStream(new File("./temp/doc_download/" + pLocation.getPath()));
				}

				@Override
				public Collection<ResourceLocation> getResources(ResourcePackType pType, String pNamespace, String pPath, int pMaxDepth, Predicate<String> pFilter){
					return MAP.values();
				}

				@Override
				public boolean hasResource(ResourcePackType pType, ResourceLocation pLocation){
					return MAP.containsValue(pLocation);
				}

				@Override
				public Set<String> getNamespaces(ResourcePackType pType){
					return KEY;
				}

				@Nullable
				@Override
				public <T> T getMetadataSection(IMetadataSectionSerializer<T> pDeserializer) throws IOException {
					return null;
				}

				@Override
				public String getName(){
					return "[FCL] External Textures";
				}

				@Override
				public void close(){
					//
				}

			});
			added = true;
		}
		File file = new File("./temp/doc_download/" + texture.getPath());
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		file.deleteOnExit();
		/*(HttpURLConnection conn = null;
		try{
			conn = (HttpURLConnection)(new URL(url).openConnection(Minecraft.getInstance().getProxy()));
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.connect();
			if(conn.getResponseCode() == 200){
				FileUtils.copyInputStreamToFile(conn.getInputStream(), file);
			}
		}
		catch(Exception exception){
			Documents.LOGGER.error("Error downloading texture from URL", (Throwable)exception);
			return texture;
		}
		finally {
			if(conn != null) conn.disconnect();
		})*/
		PlayerRenderer re;
		Minecraft.getInstance().textureManager.register(texture, new DownloadingTexture(file, url, texture, false, null));
		return texture;
	}

}
