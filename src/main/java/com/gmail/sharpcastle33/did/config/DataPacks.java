package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class DataPacks {
	private static final SortedMap<String, InputStreamSupplier> assets = new TreeMap<>();
	private static final SortedSet<String> namespaces = new TreeSet<>();

	public static void reload() {
		Bukkit.getLogger().info("Reloading DescentIntoDarkness datapacks...");
		assets.clear();
		namespaces.clear();
		int datapackCount = 0;
		try {
			Path datapacksFolder = DescentIntoDarkness.instance.getDataFolder().toPath().resolve("datapacks");
			Files.createDirectories(datapacksFolder);

			for (Path datapackFile : (Iterable<Path>) Files.list(datapacksFolder)::iterator) {
				if (Files.isDirectory(datapackFile)) {
					for (Path file : (Iterable<Path>) Files.walk(datapackFile)::iterator) {
						if (Files.isDirectory(file)) {
							Path relativePath = datapackFile.relativize(file);
							if (relativePath.getNameCount() == 2 && "data".equals(relativePath.getName(0).toString())) {
								namespaces.add(relativePath.getName(1).toString());
							}
						} else {
							assets.put(datapackFile.relativize(file).toString().replace(File.separator, "/"), () -> Files.newInputStream(file));
						}
					}
					datapackCount++;
				} else if (datapackFile.getFileName().toString().endsWith(".zip")) {
					try {
						ZipFile zip = new ZipFile(datapackFile.toFile());
						Enumeration<? extends ZipEntry> entries = zip.entries();
						while (entries.hasMoreElements()) {
							ZipEntry entry = entries.nextElement();
							if (entry.isDirectory()) {
								String name = entry.getName();
								if (name.startsWith("data/") && !"data/".equals(name)) {
									name = name.substring("data/".length(), name.length() - 1);
									if (!name.contains("/")) {
										namespaces.add(name);
									}
								}
							} else {
								assets.put(entry.getName(), () -> zip.getInputStream(entry));
							}
						}
						datapackCount++;
 					} catch (ZipException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Corrupted datapack file: " + datapackFile.getFileName(), e);
					}
				}
			}
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error reloading DescentIntoDarkness datapacks", e);
		}
		Bukkit.getLogger().info("Reloaded " + datapackCount + " DescentIntoDarkness datapacks");
	}

	public static Iterable<String> getNamespaces() {
		return namespaces;
	}

	public static List<String> getAssetsUnder(String prefix) {
		List<String> ret = new ArrayList<>();
		// Assets starting with this prefix will be greater than or equal to the prefix, no assets will be between the prefix and the relevant assets
		for (String asset : assets.tailMap(prefix).keySet()) {
			if (!asset.startsWith(prefix)) {
				break;
			}
			ret.add(asset);
		}
		return ret;
	}

	public static InputStream getInputStream(String asset) throws IOException {
		InputStreamSupplier supplier = assets.get(asset);
		if (supplier == null) {
			throw new FileNotFoundException("Datapack asset: " + asset);
		}
		return supplier.getInputStream();
	}

	@FunctionalInterface
	private interface InputStreamSupplier {
		InputStream getInputStream() throws IOException;
	}
}
