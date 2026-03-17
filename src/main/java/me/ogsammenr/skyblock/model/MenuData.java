package me.ogsammenr.skyblock.model;

import java.util.List;
import java.util.Map;

public class MenuData {
    public String title;
    public int rows;
    public Map<String, MenuItem> items;

    public static class MenuItem {
        public String id;
        public int amount;
        public String name;
        public List<String> lore;
        public String action;
        public boolean hasPlaceholder;
    }

    @Override
    public String toString() {
        StringBuilder itemsStr = new StringBuilder();
        for (Map.Entry<String, MenuItem> entry : items.entrySet()) {
            MenuItem item = entry.getValue();
            itemsStr.append("  ").append(entry.getKey()).append(": {")
                    .append("id='").append(item.id).append('\'')
                    .append(", amount=").append(item.amount)
                    .append(", Name='").append(item.name).append('\'')
                    .append(", lore=").append(item.lore)
                    .append(", action='").append(item.action).append('\'')
                    .append(", hasPlaceholder=").append(item.hasPlaceholder)
                    .append("}\n");
        }
        return "MenuData{" +
                "title='" + title + '\'' +
                ", rows=" + rows +
                ", items=\n" + itemsStr.toString() +
                '}';
    }
}
