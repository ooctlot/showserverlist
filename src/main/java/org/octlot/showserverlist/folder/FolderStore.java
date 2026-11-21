package org.octlot.showserverlist.folder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the folders of the multiplayer list and which server (by address) lives in which folder.
 * <p>
 * The vanilla {@code servers.dat} has no notion of folders, so this is stored separately in
 * {@code config/showserverlist/folders.json}. The order of servers inside a folder is the order of the
 * vanilla server list.
 */
public final class FolderStore {
    public static final int MAX_NAME_LENGTH = 32;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static @Nullable FolderStore instance;

    private final Path file;
    private final List<ServerFolder> folders = new ArrayList<>();
    /** server address -> folder id */
    private final Map<String, String> assignments = new LinkedHashMap<>();

    private FolderStore(Path file) {
        this.file = file;
        load();
    }

    public static FolderStore get() {
        if (instance == null) {
            instance = new FolderStore(FabricLoader.getInstance().getConfigDir().resolve("showserverlist").resolve("folders.json"));
        }
        return instance;
    }

    public List<ServerFolder> folders() {
        return Collections.unmodifiableList(folders);
    }

    public boolean hasFolders() {
        return !folders.isEmpty();
    }

    public @Nullable ServerFolder folderOf(String serverIp) {
        String id = assignments.get(serverIp);
        if (id == null) {
            return null;
        }
        for (ServerFolder folder : folders) {
            if (folder.id().equals(id)) {
                return folder;
            }
        }
        return null;
    }

    public int countIn(ServerFolder folder) {
        int count = 0;
        for (String id : assignments.values()) {
            if (id.equals(folder.id())) {
                count++;
            }
        }
        return count;
    }

    /** A name is available if it is not blank and no other folder uses it (ignoring case). */
    public boolean isNameAvailable(String name, @Nullable ServerFolder ignored) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (ServerFolder folder : folders) {
            if (folder != ignored && folder.name().equalsIgnoreCase(trimmed)) {
                return false;
            }
        }
        return true;
    }

    public ServerFolder create(String name) {
        ServerFolder folder = new ServerFolder(UUID.randomUUID().toString(), name.trim());
        folders.add(folder);
        save();
        return folder;
    }

    public void rename(ServerFolder folder, String name) {
        folder.setName(name.trim());
        save();
    }

    /** Removes the folder; its servers move back to the top level of the list. */
    public void delete(ServerFolder folder) {
        folders.remove(folder);
        assignments.values().removeIf(id -> id.equals(folder.id()));
        save();
    }

    public void toggleCollapsed(ServerFolder folder) {
        folder.setCollapsed(!folder.collapsed());
        save();
    }

    /** Moves a folder up ({@code -1}) or down ({@code 1}). Returns whether anything changed. */
    public boolean move(ServerFolder folder, int delta) {
        int from = folders.indexOf(folder);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= folders.size()) {
            return false;
        }
        Collections.swap(folders, from, to);
        save();
        return true;
    }

    /** Moves a folder in front of {@code before}, or to the end when {@code before} is null. */
    public boolean moveBefore(ServerFolder folder, @Nullable ServerFolder before) {
        int from = folders.indexOf(folder);
        if (from < 0) {
            return false;
        }
        folders.remove(from);
        int to = before == null ? -1 : folders.indexOf(before);
        folders.add(to < 0 ? folders.size() : to, folder);
        save();
        return true;
    }

    /** Puts a server into a folder, or back to the top level when {@code folder} is null. */
    public void assign(String serverIp, @Nullable ServerFolder folder) {
        if (folder == null) {
            assignments.remove(serverIp);
        } else {
            assignments.put(serverIp, folder.id());
        }
        save();
    }

    /** Keeps the folder of a server whose address was edited. */
    public void migrateServer(String oldIp, String newIp) {
        if (oldIp.equals(newIp)) {
            return;
        }
        String id = assignments.remove(oldIp);
        if (id != null) {
            assignments.put(newIp, id);
            save();
        }
    }

    /** Drops assignments of servers that no longer exist. */
    public void prune(Collection<String> knownServerIps) {
        if (knownServerIps.isEmpty()) {
            // An empty list usually means servers.dat failed to load - never wipe the folders for that.
            return;
        }
        Set<String> known = new HashSet<>(knownServerIps);
        if (assignments.keySet().removeIf(ip -> !known.contains(ip))) {
            save();
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null) {
                return;
            }
            Set<String> ids = new HashSet<>();
            if (data.folders != null) {
                for (ServerFolder folder : data.folders) {
                    if (folder != null && folder.id() != null && folder.name() != null && ids.add(folder.id())) {
                        folders.add(folder);
                    }
                }
            }
            if (data.assignments != null) {
                data.assignments.forEach((ip, id) -> {
                    if (ids.contains(id)) {
                        assignments.put(ip, id);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read {}, keeping a backup and starting without folders", file, e);
            backUpBrokenFile();
            folders.clear();
            assignments.clear();
        }
    }

    private void backUpBrokenFile() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up {}", file, e);
        }
    }

    private void save() {
        Data data = new Data();
        data.folders = new ArrayList<>(folders);
        data.assignments = new LinkedHashMap<>(assignments);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not save {}", file, e);
        }
    }

    private static final class Data {
        List<ServerFolder> folders = new ArrayList<>();
        Map<String, String> assignments = new LinkedHashMap<>();
    }
}
