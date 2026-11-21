package org.octlot.showserverlist.folder;

/**
 * A named, collapsible group of servers in the multiplayer list.
 * Instances are owned and persisted by {@link FolderStore}.
 */
public final class ServerFolder {
    private String id;
    private String name;
    private boolean collapsed;

    ServerFolder(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean collapsed() {
        return collapsed;
    }

    void setName(String name) {
        this.name = name;
    }

    void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
}
