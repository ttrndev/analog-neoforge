package dev.mrturtle.analog.audio.assets;

import dev.mrturtle.analog.Analog;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MusicAssetManager {
	// GitHub repo to download assets from
	// https://github.com/InventivetalentDev/minecraft-assets
	private static final String ASSET_REPO = "InventivetalentDev/minecraft-assets";

	private static final String ASSETS_ZIP_PATH = "analog/assets.zip";
	private static final String ASSETS_RECORDS_PATH = "analog/records";

	private static final String RECORDS_PATH = "assets/minecraft/sounds/records/";

	public static boolean recordsLoaded = false;

	public static void serverStarted(MinecraftServer server) {
		new Thread(() -> {
			if (!FMLPaths.CONFIGDIR.get().resolve(ASSETS_RECORDS_PATH).toFile().exists()) {
				if (!FMLPaths.CONFIGDIR.get().resolve(ASSETS_ZIP_PATH).toFile().exists()) {
					downloadAssets(server);
				} else {
					processAssets();
				}
			} else {
				recordsLoaded = true;
			}
		}).start();
	}

	private static void processAssets() {
		Analog.LOGGER.info("Processing assets for music disc playback, this may take some time...");

		File file = FMLPaths.CONFIGDIR.get().resolve(ASSETS_ZIP_PATH).toFile();
		try (ZipFile zip = new ZipFile(file)) {
			FileHeader rootFolderHeader = zip.getFileHeaders().get(0);
			String exportPath = FMLPaths.CONFIGDIR.get().resolve(ASSETS_RECORDS_PATH).getParent().toAbsolutePath().toString();
			zip.extractFile(rootFolderHeader.getFileName() + RECORDS_PATH, exportPath, "records");
			Analog.LOGGER.info("Successfully processed assets for music disc playback!");
			recordsLoaded = true;
			file.delete();
		} catch (Exception e) {
			Analog.LOGGER.error("Failed to process assets for music disc playback!");
			e.printStackTrace();
		}
	}

	private static void downloadAssets(MinecraftServer server) {
		Analog.LOGGER.info("Downloading assets for music disc playback, this may take some time...");

		String versionString = server.getServerVersion();
		try {
			HttpClient httpClient = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build();

			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI("https://api.github.com/repos/%s/zipball/refs/heads/%s".formatted(ASSET_REPO, versionString)))
					.header("X-GitHub-Api-Version", "2022-11-28")
					.GET()
					.build();

			httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenAccept((response) -> {
				try (InputStream stream = response.body()) {
					byte[] bytes = stream.readAllBytes();

					File analogDirectory = FMLPaths.CONFIGDIR.get().resolve(ASSETS_ZIP_PATH).getParent().toFile();
					if (!analogDirectory.mkdirs())
						return;

					File file = FMLPaths.CONFIGDIR.get().resolve(ASSETS_ZIP_PATH).toFile();

					OutputStream outputStream = new FileOutputStream(file);
					outputStream.write(bytes);
					outputStream.close();
					Analog.LOGGER.info("Successfully downloaded assets for music disc playback!");
					processAssets();
				} catch (IOException e) {
					Analog.LOGGER.error("Failed to save assets for music disc playback!");
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			Analog.LOGGER.error("Failed to download assets for music disc playback!");
			e.printStackTrace();
		}
	}
}
